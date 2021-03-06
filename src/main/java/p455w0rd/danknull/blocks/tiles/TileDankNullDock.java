package p455w0rd.danknull.blocks.tiles;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import mcjty.theoneprobe.Tools;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import p455w0rd.danknull.api.IRedstoneControllable;
import p455w0rd.danknull.api.ITOPBlockDisplayOverride;
import p455w0rd.danknull.init.ModBlocks;
import p455w0rd.danknull.integration.TOP;
import p455w0rd.danknull.inventory.InventoryDankNull;
import p455w0rd.danknull.util.DankNullUtils;
import p455w0rdslib.util.ItemUtils;

/**
 * @author p455w0rd
 *
 */
public class TileDankNullDock extends TileEntity implements IRedstoneControllable, ITOPBlockDisplayOverride, ISidedInventory {

	private static final String TAG_REDSTONEMODE = "RedstoneMode";
	private static final String TAG_HAS_REDSTONE_SIGNAL = "HasRSSignal";
	public static final String TAG_ITEMSTACK = "DankNullStack";
	public static final String TAG_NAME = "PWDock";
	private static final String TAG_EXTRACTMODE = "ExtractMode";
	private static final String TAG_SELECTEDSTACK = "SelectedStack";
	private static ItemStack selectedStack = ItemStack.EMPTY;

	private RedstoneMode redstoneMode = RedstoneMode.REQUIRED;
	private ExtractionMode extractMode = ExtractionMode.SELECTED;
	private boolean hasRedstoneSignal = false;
	private ItemStack dankNullStack = ItemStack.EMPTY;
	private NonNullList<ItemStack> slots = NonNullList.create();
	InventoryDankNull inventory;

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return (!getStack().isEmpty() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == EnumFacing.DOWN) || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (!getStack().isEmpty() && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing == EnumFacing.DOWN) {
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new SidedInvWrapper(this, facing));
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean overrideStandardInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState state, IProbeHitData data) {
		if (state.getBlock() == ModBlocks.DANKNULL_DOCK) {
			ItemStack stack = new ItemStack(ModBlocks.DANKNULL_DOCK);
			TileEntity tile = world.getTileEntity(data.getPos());
			if (tile != null && tile instanceof TileDankNullDock) {
				TileDankNullDock te = (TileDankNullDock) tile;
				NBTTagCompound nbttagcompound = new NBTTagCompound();
				te.writeToNBT(nbttagcompound);
				stack.setTagInfo("BlockEntityTag", nbttagcompound);
				if (player.isSneaking()) {
					probeInfo.horizontal().item(stack).vertical().itemLabel(stack).text("Extraction Mode: " + (te.getExtractionMode() == ExtractionMode.SELECTED ? "Only Selected Item" : "All Items") + " Extracted").text(" ").text("Right-click with empty hand").text("to change extraction mode").text(TextStyleClass.MODNAME.toString() + Tools.getModName(state.getBlock()));
				}
				else {
					probeInfo.horizontal().item(stack).vertical().itemLabel(stack).text(TOP.doSneak).text(TextStyleClass.MODNAME.toString() + Tools.getModName(state.getBlock()));
				}
				return true;
			}
		}
		return false;
	}

	public InventoryDankNull getInventory() {
		if (inventory == null && !getStack().isEmpty()) {
			inventory = DankNullUtils.getNewDankNullInventory(getStack());
		}
		return inventory;
	}

	public void resetInventory() {
		inventory = null;
	}

	public void setStack(@Nonnull ItemStack stack) {
		dankNullStack = stack;
		markDirty();
	}

	public int slotCount() {
		return !getStack().isEmpty() ? getStack().getItemDamage() + 1 * 9 : 0;
	}

	public ExtractionMode getExtractionMode() {
		return extractMode;
	}

	public void setExtractionMode(ExtractionMode mode) {
		extractMode = mode;
	}

	public ItemStack getSelectedStack() {
		return selectedStack;
	}

	public void setSelectedStack(ItemStack stack) {
		selectedStack = stack;
	}

	public ItemStack getStack() {
		return dankNullStack;
	}

	public NonNullList<ItemStack> getSlots() {
		return slots;
	}

	@Override
	public RedstoneMode getRedstoneMode() {
		return redstoneMode;
	}

	@Override
	public void setRedstoneMode(RedstoneMode mode) {
		redstoneMode = mode;
		markDirty();
	}

	@Override
	public boolean isRedstoneRequirementMet() {
		switch (getRedstoneMode()) {
		default:
		case IGNORED:
			return true;
		case REQUIRED:
			return hasRSSignal();
		case REQUIRE_NONE:
			return !hasRSSignal();
		}
	}

	@Override
	public boolean hasRSSignal() {
		return hasRedstoneSignal;
	}

	@Override
	public void setRSSignal(boolean isPowered) {
		hasRedstoneSignal = isPowered;
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
		return oldState.getBlock() != newSate.getBlock();
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	@Nullable
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 255, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void markDirty() {
		super.markDirty();
		if (getWorld() != null) {
			IBlockState state = getWorld().getBlockState(pos);
			if (state != null) {
				getWorld().notifyBlockUpdate(pos, state, state, 3);
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);

		NBTTagCompound nbt = compound.getCompoundTag(TAG_NAME);
		setRedstoneMode(RedstoneMode.values()[nbt.getInteger(TAG_REDSTONEMODE)]);
		setRSSignal(nbt.getBoolean(TAG_HAS_REDSTONE_SIGNAL));
		NBTTagCompound itemNBT = compound.getCompoundTag(TAG_ITEMSTACK);
		ItemStack newStack = itemNBT == null ? ItemStack.EMPTY : new ItemStack(itemNBT);
		setStack(newStack);
		setExtractionMode(ExtractionMode.values()[compound.getInteger(TAG_EXTRACTMODE)]);
		if (compound.hasKey(TAG_SELECTEDSTACK) && getExtractionMode() == ExtractionMode.SELECTED) {
			setSelectedStack(new ItemStack(compound.getCompoundTag(TAG_SELECTEDSTACK)));
		}

	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound = super.writeToNBT(compound);
		NBTTagCompound itemNBT = new NBTTagCompound();
		compound.setInteger(TAG_REDSTONEMODE, redstoneMode.ordinal());
		compound.setBoolean(TAG_HAS_REDSTONE_SIGNAL, hasRSSignal());
		compound.setInteger(TAG_EXTRACTMODE, getExtractionMode().ordinal());
		if (!getSelectedStack().isEmpty()) {
			NBTTagCompound selectedItemNBT = new NBTTagCompound();
			getSelectedStack().writeToNBT(selectedItemNBT);
			compound.setTag(TAG_SELECTEDSTACK, selectedItemNBT);
		}
		if (!getStack().isEmpty()) {
			getStack().writeToNBT(itemNBT);
			compound.setTag(TAG_ITEMSTACK, itemNBT);
		}
		return compound;
	}

	public static enum ExtractionMode {
			NORMAL, SELECTED;
	}

	@Override
	public int getSizeInventory() {
		return !getStack().isEmpty() ? getInventory().getSizeInventory() : 0;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		return !getStack().isEmpty() ? getInventory().getStackInSlot(index) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		ItemStack ret = !getStack().isEmpty() ? getInventory().decrStackSize(index, count) : ItemStack.EMPTY;
		if (!getStackInSlot(index).isEmpty()) {
			DankNullUtils.reArrangeStacks(getInventory());
		}
		return ret;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		if (!getStack().isEmpty()) {
			getInventory().setInventorySlotContents(index, stack);
		}
	}

	@Override
	public int getInventoryStackLimit() {
		return !getStack().isEmpty() ? Integer.MAX_VALUE : 0;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return true;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return false;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
	}

	@Override
	public String getName() {
		return "danknull-inventory";
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		if (!getStack().isEmpty() && side == EnumFacing.DOWN) {
			int[] slots = new int[getSizeInventory()];
			for (int i = 0; i < slots.length; i++) {
				slots[i] = i;
			}
			return slots;
		}
		return new int[0];
	}

	@Override
	public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
		return false;
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
		if (!getStack().isEmpty()) {
			if (getExtractionMode() == ExtractionMode.NORMAL) {
				return true;
			}
			else if (getExtractionMode() == ExtractionMode.SELECTED) {
				if (ItemUtils.areItemsEqual(getInventory().getStackInSlot(index), getSelectedStack())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return !getStack().isEmpty();
	}

	public enum RedstoneMode {
			REQUIRED, REQUIRE_NONE, IGNORED
	}

}
