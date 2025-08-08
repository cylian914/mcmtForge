package net.tessa.mcmtforge.debug;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MSPT10DebugEntity extends Mob {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long TARGET_TIME_MS = 10;
    private static volatile double result; // Prevent JVM optimization
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(MSPT10DebugEntity.class, EntityDataSerializers.BOOLEAN);

    public MSPT10DebugEntity(EntityType<? extends MSPT10DebugEntity> entityType, Level world) {
        super(entityType, world);
        LOGGER.info("Creating new MSPT10DebugEntity");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1.0) // Only 1 health point
                .add(Attributes.MOVEMENT_SPEED, 0.0) // Doesn't move
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0) // Doesn't get knocked back
                .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTIVE, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getEntityData().get(ACTIVE)) {
            spinCPU(TARGET_TIME_MS);
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return null;
    }

    private static void spinCPU(long targetMs) {
        long startTime = System.nanoTime();
        long targetNanos = targetMs * 1_000_000;
        double x = Math.PI;

        while (System.nanoTime() - startTime < targetNanos) {
            x = Math.sin(x) * Math.cos(x);
            x = Math.pow(x, 1.1);
            x = Math.sqrt(Math.abs(x));
            x += Math.PI;
            result = x;
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("Active")) {
            getEntityData().set(ACTIVE, nbt.getBoolean("Active"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Active", getEntityData().get(ACTIVE));
    }

    // Required methods for LivingEntity
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
}