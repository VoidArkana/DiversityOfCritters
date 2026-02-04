package com.evirapo.diversityofcritters.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.List;
import java.util.Set;

public class DiversityMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        System.out.println("[DOC-DEBUG] DiversityMixinPlugin cargado. Paquete: " + mixinPackage);
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // AHORA PROTEGEMOS CUALQUIER MIXIN QUE ESTÉ EN EL PAQUETE "compat" Y TENGA "Carry" EN EL NOMBRE
        if (mixinClassName.contains("compat.Carry")) {
            boolean isCarryOnLoaded = FMLLoader.getLoadingModList().getModFileById("carryon") != null;
            return isCarryOnLoaded;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override
    public List<String> getMixins() { return null; }
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}