package github.xniter.dtmintegrations.mixin.sevendaystomine;

import github.xniter.dtmintegrations.handlers.config.ConfigGetter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;
import nuparu.sevendaystomine.entity.EntityAirdrop;
import nuparu.sevendaystomine.inventory.itemhandler.AirdropInventoryHandler;
import nuparu.sevendaystomine.util.MathUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.ParametersAreNonnullByDefault;

@Mixin(value = EntityAirdrop.class, remap = false)
public abstract class MixinEntityAirdrop extends Entity {

    @Shadow
    private ItemStackHandler inventory;

    @Shadow
    public long age;

    private static final DataParameter<Integer> intAge = EntityDataManager.<Integer>createKey(EntityAirdrop.class, DataSerializers.VARINT);

    private static final DataParameter<Boolean> DROPPED = EntityDataManager.<Boolean>createKey(EntityAirdrop.class, DataSerializers.BOOLEAN);

    private static final DataParameter<Integer> DROP_TIME = EntityDataManager.<Integer>createKey(EntityAirdrop.class, DataSerializers.VARINT);

    private static final DataParameter<Float> START_POS = EntityDataManager.<Float>createKey(EntityAirdrop.class, DataSerializers.FLOAT);

    @Shadow
    @Final
    private static DataParameter<Boolean> LANDED;

    @Shadow
    @Final
    private static DataParameter<Integer> SMOKE_TIME;

    @Shadow
    @Final
    private static DataParameter<Integer> HEALTH;

    private static EntityAirdrop airDrop;

    public MixinEntityAirdrop(World worldIn) {
        super(worldIn);
    }

    /**
     * @author X_Niter
     * @reason Airdrop Modifications
     */
    @Overwrite(remap = false)
    public void onUpdate() {
        ignoreFrustumCheck = true;
        super.onUpdate();

        boolean dropped = this.isDropped();

        int intAge = this.getAge();

        BlockPos bP1 = this.getPosition().down(1);
        this.onGround = !this.world.isAirBlock(bP1);



        if (!this.onGround && !this.getLanded() && !dropped && intAge >= 0) {
            this.Dropped();
        }

        if (ConfigGetter.getAirdropGlowingEnabled()) {
            this.glowing = true;
        }

        if (dropped) {
            int dropTime = getDropTime();

            float startPos = this.getStartPos();

            this.motionY = -ConfigGetter.getAirdropFallingSpeed();

            this.setPosition(this.posX, startPos - Math.pow(getDropTime(), 1) / 6, this.posZ);

            this.setDropTime(dropTime + 1);
        }

        if (this.onGround) {
            this.age = 0;

            ++this.age;

            this.motionY = 0.0;

            this.setPosition(this.getPosition().getX(), this.getPosition().getY(), this.getPosition().getZ());


            if (age >= 48000L || this.posY < 1 || this.posY > 256 || intAge >= 48000) {
                this.setDead();

                MinecraftServer server = Minecraft.getMinecraft().world.getMinecraftServer();

                if (server != null && server.getPlayerList().getCurrentPlayerCount() != 0) {
                    if (ConfigGetter.getUseLangConfig()) {
                        server.getPlayerList().sendMessage(new TextComponentTranslation(ConfigGetter.getAirdropDespawnMessage(), world.getWorldInfo().getWorldName(), this.getPosition().getX(), this.getPosition().getZ()));
                    } else {
                        server.getPlayerList().sendMessage(new TextComponentTranslation("airdrop.removed.message", world.getWorldInfo().getWorldName(), this.getPosition().getX(), this.getPosition().getZ()));
                    }
                }
            }

            if (!this.getLanded()) {
                this.setSmokeTime(1200);
                this.setDropped(false);
                this.setLanded(true);
            }


//            if (!this.getLanded()) {
//                this.motionY = -ConfigGetter.getAirdropFallingSpeed() / 1.5;
//
//            } else {
//                this.motionY = -ConfigGetter.getAirdropFallingSpeed() / 2;
//
//            }

        }



        if (this.onGround && !dropped && this.getLanded() && this.getSmokeTime() > 0) {
            for (int i = 0; i < this.world.rand.nextInt(4) + 1; ++i) {
                this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, this.posX, this.posY + (double) this.height, this.posZ, (double) MathUtils.getFloatInRange(-0.1F, 0.1F), (double) MathUtils.getFloatInRange(0.2F, 0.5F), (double) MathUtils.getFloatInRange(-0.1F, 0.1F));
                this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, this.posX, this.posY + (double) this.height, this.posZ, (double) MathUtils.getFloatInRange(-0.1F, 0.1F), (double) MathUtils.getFloatInRange(0.2F, 0.5F), (double) MathUtils.getFloatInRange(-0.1F, 0.1F));
            }

            this.setSmokeTime(this.getSmokeTime() - 1);
        }


        //this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        this.setAge(intAge + 1);

    }

    /**
     * @author X_Niter
     * @reason Update Initialized data
     */
    @Overwrite(remap = false)
    protected void entityInit() {
        this.dataManager.register(intAge, 0);
        this.dataManager.register(LANDED, false);
        this.dataManager.register(SMOKE_TIME, 0);
        this.dataManager.register(START_POS, 0.F);
        this.dataManager.register(DROPPED, false);
        this.dataManager.register(DROP_TIME, 0);
        this.dataManager.register(HEALTH, 50);
    }

    public int getDropTime(){
        return this.dataManager.get(DROP_TIME);
    }

    public void setDropTime(Integer dropTime){
        this.dataManager.set(DROP_TIME, dropTime);
    }

    public boolean isDropped(){
        return this.dataManager.get(DROPPED);
    }

    public void setDropped(boolean dropped){
        this.dataManager.set(DROPPED, dropped);
    }

    public void Dropped(){
        ignoreFrustumCheck = true;
        this.setDropped(true);
        this.isAirBorne = true;
        this.setStartPos((float)this.posY);
    }

    public int getAge(){
        return this.dataManager.get(intAge);
    }

    public void setAge(Integer Age){
        this.dataManager.set(intAge, Age);
    }

    public float getStartPos(){
        return this.dataManager.get(START_POS);
    }

    public void setStartPos(Float startPos){
        this.dataManager.set(START_POS, startPos);
    }

    @Override
    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos){}

    @Shadow
    public void setLanded(boolean landed) {
        this.dataManager.set(LANDED, landed);
    }

    @Shadow
    public boolean getLanded() {
        return this.dataManager.get(LANDED);
    }

    @Shadow
    public void setSmokeTime(int ticks) {
        this.dataManager.set(SMOKE_TIME, ticks);
    }

    @Shadow
    public int getSmokeTime() {
        return this.dataManager.get(SMOKE_TIME);
    }

    /**
     * @author X_Niter
     * @reason Update Initialized data
     */
    @Overwrite(remap = false)
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.inventory.deserializeNBT(compound.getCompoundTag("inventory"));

        this.setLanded(compound.getBoolean("landed"));

        this.setSmokeTime(compound.getInteger("smoke_time"));

        this.setAge(compound.getInteger("intage"));

        this.age = compound.getLong("age");

        this.setStartPos(compound.getFloat("StartPos"));

        this.setDropped(compound.getBoolean("Dropped"));

        this.setDropTime(compound.getInteger("DropTime"));

    }

    /**
     * @author X_Niter
     * @reason Update Initialized data
     */
    @Overwrite(remap = false)
    @ParametersAreNonnullByDefault
    public void writeEntityToNBT(NBTTagCompound compound) {
        if (this.inventory != null) {
            compound.setTag("inventory", this.inventory.serializeNBT());
        }

        compound.setBoolean("landed", this.getLanded());
        compound.setInteger("smoke_time", this.getSmokeTime());
        compound.setInteger("intage", this.getAge());
        compound.setLong("age", this.age);
        compound.setFloat("StartPos", this.getStartPos());
        compound.setBoolean("Dropped", this.isDropped());
        compound.setInteger("DropTime", this.getDropTime());
    }

    /**
     * @author X_Niter
     * @reason Render Changes
     */
    @Overwrite(remap = false)
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return super.isInRangeToRenderDist(distance);
    }

    /**
     * @author X_Niter
     * @reason When the Airdrop is emptied of the loot, then delete the airdrop
     */
    @Overwrite(remap = false)
    public void onInventoryChanged(AirdropInventoryHandler airdropInventoryHandler) {
        for(int j = 0; j < this.inventory.getSlots(); ++j) {
            ItemStack stack = this.inventory.getStackInSlot(j);
            if (!stack.isEmpty()) {
                return;
            } else {
                if (this.onGround && this.getLanded()) {

                    if (this.getServer().getEntityWorld() != null) {
                        this.getServer().getEntityWorld().removeEntity(this);
                    } else {
                        this.setDead();
                    }

                    this.world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, this.posX, this.posY, this.posZ, 1, 1, 1, 1);
                }
            }
        }

        //age = MathUtils.clamp(age, 42000, 48000);
    }
}
