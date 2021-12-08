/*
 * Copyright (c) 2021 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.projectreactor.bot.github

import io.netty.buffer.ByteBufUtil
import org.springframework.beans.factory.annotation.Autowired
import io.projectreactor.bot.config.GitHubProperties
import org.springframework.web.server.WebFilter
import javax.crypto.spec.SecretKeySpec
import java.io.UnsupportedEncodingException
import org.springframework.beans.factory.BeanInitializationException
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.lang.Void
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import javax.crypto.Mac
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import reactor.core.publisher.Flux
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import org.springframework.util.PathMatcher
import reactor.util.annotation.NonNull
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * @author Simon Baslé
 */
@Component
class GithubHookFilter @Autowired internal constructor(properties: GitHubProperties) : WebFilter {
    private var hookSecretKey: SecretKeySpec? = null

    init {
        val key = properties.hookSecret
        try {
            hookSecretKey = SecretKeySpec(key!!.toByteArray(charset("UTF-8")), "HmacSHA256")
        } catch (e: UnsupportedEncodingException) {
            throw BeanInitializationException("Unable to prepare hook secret", e)
        }
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!exchange.request.path.value().startsWith("/gh/") || exchange.request.method == HttpMethod.GET) {
            return chain.filter(exchange)
        }

        val digestGhString = exchange.request.headers.getFirst("X-Hub-Signature-256")
            ?: return Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED, "request not signed"))
        val digestGh = digestGhString.replaceFirst("256=", "").toByteArray(StandardCharsets.UTF_8)

        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(hookSecretKey)

            val hmacVerifDecorator: ServerHttpRequestDecorator = object : ServerHttpRequestDecorator(exchange.request) {
                @NonNull
                override fun getBody(): Flux<DataBuffer> {
                    return DataBufferUtils.join(
                        super.getBody()
                            .doOnNext { buf: DataBuffer -> mac.update(buf.asByteBuffer()) }
                    )
                        .map { buf: DataBuffer ->
                            val bodyDigest = mac.doFinal()
                            val bodyDigestHex = ByteBufUtil.hexDump(bodyDigest).toByteArray()
                            if (!MessageDigest.isEqual(bodyDigestHex, digestGh)) {
                                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad signature")
                            }
                            buf
                        }
                        .flux()
                }
            }
            chain.filter(
                exchange.mutate()
                    .request(hmacVerifDecorator)
                    .build()
            )
        } catch (e: NoSuchAlgorithmException) {
            Mono.error(ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hmac-SHA256 not available", e))
        } catch (e: InvalidKeyException) {
            Mono.error(ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hmac-SHA256 invalid key", e))
        }
    }
}