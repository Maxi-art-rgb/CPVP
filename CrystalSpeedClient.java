package com.example.crystalspeed;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class CrystalSpeedClient implements ClientModInitializer {

    // Stufe 0 = x1 (normal), 1 = x2, 2 = x3, 3 = x4
    private static final String[] NAMES = {"x1", "x2", "x3", "x4"};
    private static final int[] CPS = {5, 10, 15, 20};

    private KeyMapping cycleKey;
    private KeyMapping hudKey;

    private int level = 0;
    private boolean hudVisible = true;

    private double useAcc = 0;
    private double attackAcc = 0;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("crystalspeed", "main"));

        // Beide Tasten erscheinen unter Optionen -> Steuerung -> "Crystal Speed" und sind frei belegbar
        cycleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.crystalspeed.cycle", InputConstants.Type.KEYSYM, InputConstants.KEY_V, category));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.crystalspeed.hud", InputConstants.Type.KEYSYM, InputConstants.KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("crystalspeed", "level"),
                (graphics, deltaTracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (!hudVisible || mc.player == null) return;
                    String text = "Crystal Speed: " + NAMES[level] + " (" + CPS[level] + " CPS)";
                    graphics.drawString(mc.font, text, 6, 6, 0xFFFFFFFF);
                });
    }

    private void onTick(Minecraft mc) {
        while (cycleKey.consumeClick()) {
            level = (level + 1) % NAMES.length;
        }
        while (hudKey.consumeClick()) {
            hudVisible = !hudVisible;
        }

        if (mc.player == null || mc.level == null || mc.gameMode == null
                || mc.screen != null || level == 0) {
            useAcc = 0;
            attackAcc = 0;
            return;
        }

        // Rechtsklick: Kristall / Anker / Glowstone
        // Vanilla klickt gehalten schon ca. 5x pro Sekunde, wir ergaenzen nur den Rest bis zum Ziel-CPS.
        Item held = mc.player.getMainHandItem().getItem();
        if (mc.options.keyUse.isDown() && isPlaceItem(held)) {
            useAcc += (CPS[level] - 5) / 20.0;
            while (useAcc >= 1) {
                useAcc -= 1;
                doUse(mc);
            }
        } else {
            useAcc = 0;
        }

        // Linksklick: Kristalle zerschlagen
        if (mc.options.keyAttack.isDown() && isCrystalTarget(mc.hitResult)) {
            attackAcc += CPS[level] / 20.0;
            while (attackAcc >= 1) {
                attackAcc -= 1;
                if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal) {
                    mc.gameMode.attack(mc.player, ehr.getEntity());
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        } else {
            attackAcc = 0;
        }
    }

    private boolean isCrystalTarget(HitResult hit) {
        return hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal;
    }

    private boolean isPlaceItem(Item item) {
        return item == Items.END_CRYSTAL || item == Items.RESPAWN_ANCHOR || item == Items.GLOWSTONE;
    }

    private void doUse(Minecraft mc) {
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
            if (result.consumesAction()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }
}
