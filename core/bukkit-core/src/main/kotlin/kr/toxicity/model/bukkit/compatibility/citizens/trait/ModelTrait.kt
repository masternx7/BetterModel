/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit.compatibility.citizens.trait

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter
import kr.toxicity.model.api.data.renderer.ModelRenderer
import net.citizensnpcs.api.trait.Trait
import net.citizensnpcs.api.trait.TraitName
import net.citizensnpcs.api.util.DataKey

@TraitName("model")
class ModelTrait : Trait("model") {
    private var _renderer: ModelRenderer? = null
    private var modelName: String? = null
    private var retryTicks = 0

    var renderer
        get() = _renderer
        set(value) {
            removeModel()
            _renderer = value
            modelName = value?.name()
            val entity = npc?.entity
            if (entity != null && value != null) {
                value.create(BukkitAdapter.adapt(entity))
            }
        }

    override fun load(key: DataKey) {
        modelName = key.getString("")?.takeIf { it.isNotEmpty() }
        _renderer = modelName?.let { runCatching { BetterModel.modelOrNull(it) }.getOrNull() }
    }

    override fun save(key: DataKey) {
        // Never read the name back from the live registry here. When the NPC is
        // despawned, BetterModel is reloading, or the model is not attached yet,
        // the lookup returns null and used to erase "model:" from saves.yml.
        key.setString("", modelName ?: _renderer?.name())
    }

    override fun run() {
        if (_renderer != null || modelName == null)
            return
        if (++retryTicks < RETRY_INTERVAL)
            return
        retryTicks = 0
        applyModel()
    }

    override fun onSpawn() {
        applyModel()
    }

    override fun onCopy() {
        onSpawn()
    }

    override fun onDespawn() {
        removeModel()
    }

    override fun onRemove() {
        removeModel()
    }

    private fun applyModel() {
        val entity = npc?.entity ?: return
        val renderer = _renderer
            ?: modelName?.let { runCatching { BetterModel.modelOrNull(it) }.getOrNull() }?.also { _renderer = it }
            ?: return
        val registry = BetterModel.registryOrNull(entity.uniqueId)
        if (registry?.tracker(renderer.name()) == null) {
            renderer.create(BukkitAdapter.adapt(entity))
        }
    }

    private fun removeModel() {
        val entity = npc?.entity ?: return
        val name = _renderer?.name() ?: modelName ?: return
        BetterModel.registryOrNull(entity.uniqueId)?.tracker(name)?.close()
    }

    private companion object {
        const val RETRY_INTERVAL = 20
    }
}
