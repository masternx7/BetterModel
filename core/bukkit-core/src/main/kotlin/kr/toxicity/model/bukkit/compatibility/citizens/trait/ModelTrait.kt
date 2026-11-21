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
    private var modelName: String? = null
    private var retryTicks = 0

    var renderer: ModelRenderer?
        get() = modelName?.let(BetterModel::modelOrNull)
        set(value) {
            removeModel()
            modelName = value?.name()
            applyModel()
        }

    override fun load(key: DataKey) {
        modelName = key.getString("")?.takeIf { it.isNotEmpty() }
    }

    override fun save(key: DataKey) {
        // Never read the name back from the live registry here. When the NPC is
        // despawned, BetterModel is reloading, or the model is not attached yet,
        // the lookup returns null and used to erase "model:" from saves.yml.
        key.setString("", modelName)
    }

    override fun run() {
        if (modelName == null || npc?.isSpawned != true)
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
        val name = modelName ?: return
        val entity = npc?.takeIf { it.isSpawned }?.entity ?: return
        val registry = BetterModel.registryOrNull(entity.uniqueId)
        // A reload temporarily closes the registry while it rebuilds its trackers.
        if (registry?.isClosed == true || registry?.tracker(name)?.isClosed == false) return
        renderer?.create(BukkitAdapter.adapt(entity))
    }

    private fun removeModel() {
        val entity = npc?.entity ?: return
        val name = modelName ?: return
        BetterModel.registryOrNull(entity.uniqueId)?.remove(name)
    }

    private companion object {
        const val RETRY_INTERVAL = 20
    }
}
