package com.example.citymod;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(CityMod.MODID)
public class CityMod {
    public static final String MODID = "citymod";

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<CityChunkGenerator>> CITY =
            CHUNK_GENERATORS.register("city", () -> CityChunkGenerator.CODEC);

    public CityMod(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
    }
}
