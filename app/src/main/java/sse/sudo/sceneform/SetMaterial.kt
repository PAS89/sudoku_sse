package sse.sudo.sceneform

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.utilities.AndroidPreconditions
import java.util.concurrent.CompletableFuture

class Setmaterial private constructor(
    val value: Material,
    private val baseColorMap: Texture?,
    private val defaultBaseColorMap: Texture?
) {
    companion object {
        inline fun build(context: Context, block: Builder.() -> Unit) = Builder(
            context
        ).apply(block).build()
    }

    var isDefaultBaseColorMap: Boolean
        private set

    init {
        isDefaultBaseColorMap = true
    }

    private fun setBaseColorMap(texture: Texture?) {
        if (texture != null) {
            value.setTexture("baseColorMap", texture)
        }
    }

    fun switchBaseColor() {
        isDefaultBaseColorMap = if (isDefaultBaseColorMap) {
            setBaseColorMap(baseColorMap)
            false
        } else {
            setBaseColorMap(defaultBaseColorMap)
            true
        }
    }



    fun reset() {
        isDefaultBaseColorMap = true
        setBaseColorMap(defaultBaseColorMap)
    }

    class Builder(val context: Context) {
        var baseColorSource: Uri? = null

        fun build(): CompletableFuture<Setmaterial> {
            AndroidPreconditions.checkUiThread()

            val renderableFuture = createMaterialRenderable()
            val defaultColorFuture = createTexture(R.raw.custom_material_default_diffuse, Texture.Usage.COLOR)
            val baseColorFuture = createTexture(baseColorSource!!, Texture.Usage.COLOR)

            return CompletableFuture.allOf(
                renderableFuture,
                baseColorFuture,
                defaultColorFuture
            ).thenApplyAsync {
                Setmaterial(
                    renderableFuture.get().material,
                    baseColorFuture.get() ,
                    defaultColorFuture.get()
                )
            }.exceptionally { ex ->
                Log.e(TAG, "Unable to create Setmaterial", ex)
                null
            }
        }

        private fun createMaterialRenderable(): CompletableFuture<ModelRenderable> = ModelRenderable.builder()
            .setSource(context, R.raw.custom_material)
            .build()
            .exceptionally { ex ->
                Log.e(TAG, "unable to load custom_material renderable", ex)
                null
            }

        private fun createTexture(sourceUri: Uri, usage: Texture.Usage): CompletableFuture<Texture> =
            Texture.builder()
                .setSource(context, sourceUri)
                .setUsage(usage)
                .setSampler(
                    Texture.Sampler.builder()
                        .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                        .setMinFilter(Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR)
                        .build()
                ).build()
                .exceptionally { ex ->
                    Log.e(TAG, "Unable to load texture from $sourceUri", ex)
                    null
                }

        private fun createTexture(id: Int, usage: Texture.Usage): CompletableFuture<Texture> = Texture.builder()
            .setSource(context, id)
            .setUsage(usage)
            .setSampler(
                Texture.Sampler.builder()
                    .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                    .setMinFilter(Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR)
                    .build()
            ).build()
    }
}