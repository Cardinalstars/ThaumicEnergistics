package thaumicenergistics.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.api.grid.ICraftingIssuerHost;
import thaumicenergistics.common.ThEGuiHandler;
import thaumicenergistics.common.items.ItemCraftingAspect;
import thaumicenergistics.common.network.packet.client.Packet_C_EssentiaCellTerminal;
import thaumicenergistics.common.network.packet.server.Packet_S_EssentiaCellTerminal;
import thaumicenergistics.common.parts.AEPartEssentiaTerminal;
import thaumicenergistics.common.storage.AspectStackComparator.AspectStackComparatorMode;
import thaumicenergistics.common.utils.EffectiveSide;
import appeng.api.AEApi;
import appeng.api.config.ViewItems;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftAmount;

/**
 * Inventory container for the essentia terminal.
 * 
 * @author Nividica
 * 
 */
public class ContainerEssentiaTerminal
	extends AbstractContainerCellTerminalBase
{
	/**
	 * The terminal this is associated with.
	 */
	private AEPartEssentiaTerminal terminal = null;

	/**
	 * Network source representing the player who is interacting with the
	 * container.
	 */
	private PlayerSource playerSource = null;

	/**
	 * Creates the container
	 * 
	 * @param terminal
	 * Parent terminal.
	 * @param player
	 * Player that owns this container.
	 */
	public ContainerEssentiaTerminal( final AEPartEssentiaTerminal terminal, final EntityPlayer player )
	{
		// Call the super
		super( player );

		// Set the player
		this.player = player;

		// Set the terminal
		this.terminal = terminal;

		// Get and set the machine source
		this.playerSource = new PlayerSource( player, this.terminal );

		// Is this server side?
		if( EffectiveSide.isServerSide() )
		{
			// Get the monitor
			this.monitor = terminal.getGridBlock().getEssentiaMonitor();

			// Attach to the monitor
			this.attachToMonitor();

			// Add this container to the terminal's list
			terminal.addListener( this );
		}
		else
		{
			// Ask for a list update
			Packet_S_EssentiaCellTerminal.sendFullUpdateRequest( this.player );
			this.hasRequested = true;
		}

		// Bind our inventory
		this.bindToInventory( terminal.getInventory() );
	}

	/**
	 * Transfers essentia.
	 */
	@Override
	public void doWork( final int elapsedTicks )
	{
		// Transfer essentia if needed.
		this.transferEssentia( this.playerSource );
	}

	@Override
	public ICraftingIssuerHost getCraftingHost()
	{
		return this.terminal;
	}

	@Override
	public void onClientRequestAutoCraft( final EntityPlayer player, final Aspect aspect )
	{
		// Get the host tile
		TileEntity te = this.terminal.getHostTile();

		// Launch the GUI
		ThEGuiHandler.launchGui( ThEGuiHandler.AUTO_CRAFTING_AMOUNT, player, te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord );

		// Setup the amount container
		if( player.openContainer instanceof ContainerCraftAmount )
		{
			// Get the container
			ContainerCraftAmount cca = (ContainerCraftAmount)this.player.openContainer;

			// Create the open context
			cca.setOpenContext( new ContainerOpenContext( te ) );
			cca.getOpenContext().setWorld( te.getWorldObj() );
			cca.getOpenContext().setX( te.xCoord );
			cca.getOpenContext().setY( te.yCoord );
			cca.getOpenContext().setZ( te.zCoord );
			cca.getOpenContext().setSide( this.terminal.getSide() );

			// Create the result item
			IAEItemStack result = AEApi.instance().storage().createItemStack( ItemCraftingAspect.createStackForAspect( aspect, 1 ) );

			// Set the item
			cca.getCraftingItem().putStack( result.getItemStack() );
			cca.setItemToCraft( result );

			// Issue update
			cca.detectAndSendChanges();
		}
	}

	/**
	 * Gets the current list from the AE monitor, as well as the current
	 * sorting mode, and sends it to the client.
	 */
	@Override
	public void onClientRequestFullUpdate()
	{
		// Send the sorting mode
		this.onModeChanged( this.terminal.getSortingMode(), this.terminal.getViewMode() );

		// Send the aspect list
		if( this.monitor != null )
		{
			Packet_C_EssentiaCellTerminal.sendFullList( this.player, this.repo.getAll() );
		}
	}

	@Override
	public void onClientRequestSortModeChange( final EntityPlayer player, final boolean backwards )
	{
		this.terminal.onClientRequestSortingModeChange( backwards );
	}

	@Override
	public void onClientRequestViewModeChange( final EntityPlayer player, final boolean backwards )
	{
		this.terminal.onClientRequestViewModeChange( backwards );
	}

	/**
	 * Removes this container from the terminal.
	 */
	@Override
	public void onContainerClosed( final EntityPlayer player )
	{
		super.onContainerClosed( player );

		if( EffectiveSide.isServerSide() && ( this.terminal != null ) )
		{
			this.terminal.removeListener( this );
		}
	}

	/**
	 * Called from the terminal when it's mode has changed
	 */
	public void onModeChanged( final AspectStackComparatorMode sortingMode, final ViewItems viewMode )
	{
		// Inform the client
		Packet_C_EssentiaCellTerminal.sendViewingModes( this.player, sortingMode, viewMode );
	}

}