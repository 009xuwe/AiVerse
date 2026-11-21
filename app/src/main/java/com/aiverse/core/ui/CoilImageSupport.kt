package com.aiverse.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.Coil
import okhttp3.OkHttpClient

/**
 * 构建全项目统一的 Coil 图片加载器。
 *
 * Coil 2.x 只加依赖（coil-gif / coil-svg）并不会自动启用对应能力，必须把解码器显式注册进
 * [ImageLoader.components]。这里集中注册：
 * - [GifDecoder]：纯软件解码，API 26/27 上没有系统 `ImageDecoder` 时的兜底；
 * - [ImageDecoderDecoder]：API 28+ 的系统 `ImageDecoder`，负责 GIF / 动图 WebP / HEIF 动图；
 * - [SvgDecoder]：把 SVG 矢量图栅格化成位图。
 *
 * 硬件位图让常规图片的像素常驻 GPU 而非 Java 堆，降低长截图 / 大图的堆压力；需要软件位图的
 * 格式（SVG 栅格、动图帧）由 Coil 自行回退到软件解码。
 */
fun buildAppImageLoader(context: Context): ImageLoader =
    ImageLoader.Builder(context)
        .allowHardware(true)
        .okHttpClient {
            // 远程图沿用旧直连解码器的 UA，避免按 UA 过滤/限流的图床返回 403。
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "AiVerse")
                            .build()
                    )
                }
                .build()
        }
        .memoryCache {
            coil.memory.MemoryCache.Builder(context)
                .maxSizePercent(0.20)
                .build()
        }
        .diskCache {
            coil.disk.DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(64L * 1024 * 1024)
                .build()
        }
        .components {
            add(GifDecoder.Factory())
            add(ImageDecoderDecoder.Factory())
            add(SvgDecoder.Factory())
        }
        .build()

/** 取进程内共享的图片加载器（App 覆写 `CoilImageLoaderFactory` 后与 Coil 默认单例同源）。 */
val Context.appImageLoader: ImageLoader
    get() = Coil.imageLoader(this)

/** 在组合里取进程内共享的图片加载器。 */
@Composable
fun rememberAppImageLoader(): ImageLoader = LocalContext.current.appImageLoader
