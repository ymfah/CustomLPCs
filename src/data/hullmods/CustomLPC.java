package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.List;

import java.awt.Color;
import java.util.Objects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

@SuppressWarnings("unchecked")
public class CustomLPC extends BaseHullMod {

	public static float CR_PERCENT = -10f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

		stats.getMaxCombatReadiness().modifyFlat(id, Math.round(CR_PERCENT * 100f) * 0.01f, "LPC designation");
		stats.getCargoMod().modifyMult(id, 0f);
		stats.getFuelMod().modifyMult(id, 0f);
		stats.getMaxCrewMod().modifyMult(id, 0f);
		stats.getMinCrewMod().modifyMult(id, 0f);
		stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 0f);
		stats.getSuppliesPerMonth().modifyMult(id, 0f);

		Global.getSettings().getDescription("customlpc", Description.Type.SHIP).setText1("A custom LPC that can be programmed to create any ship. " +
				"Currently programmed to create a " +stats.getVariant().getFullDesignationWithHullName()+".");
		Global.getSettings().getFighterWingSpec("customlpc").setOpCost(stats.getVariant().getHullSpec().getSuppliesToRecover()*5f);
		Global.getSettings().getFighterWingSpec("customlpc").setRefitTime(stats.getVariant().getHullSpec().getSuppliesToRecover()*5f);
		Global.getSettings().getHullSpec("customlpc").setShipSystemId(stats.getVariant().getHullSpec().getShipSystemId());
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}

	@Override
	public boolean affectsOPCosts() {
		return false;
	}

	private int getInterfaceCount(MutableShipStatsAPI stats) {
		int interfaceCount = 0;
		if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return interfaceCount;
		CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
		List<FleetMemberAPI> fleetList = fleet.getMembersWithFightersCopy();

		List<MarketAPI> marketList = Global.getSector().getEconomy().getMarketsCopy();
		for (MarketAPI market : marketList) {
			if (market.getSubmarket(Submarkets.SUBMARKET_STORAGE) != null) {
				CargoAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
				List<FleetMemberAPI> storageList = storage.getMothballedShips().getMembersListCopy();
				if (!storageList.isEmpty()) {
					for (FleetMemberAPI ship : storageList) fleetList.add(ship);
				}
			}
		}
		boolean playsound = false;
		for (FleetMemberAPI member : fleetList) {
			if (member.getVariant().hasHullMod("customlpc")) interfaceCount++;
			//if change causes an OP overflow, remove wing and add to cargo.
			if(member.getVariant().getUnusedOP(Global.getSector().getPlayerStats())<0){
				int i = 0;
				for(String id : member.getVariant().getWings()){
					if(Objects.equals(id, "customlpc")){
						member.getVariant().setWingId(i, null);
						fleet.getCargo().addFighters("customlpc", 1);
						playsound = true;
					}
					i++;
				}
			}
		}
		if(playsound)Global.getSoundPlayer().playUISound("ui_cargo_special_industrial_drop", 1.0F, 1.0F);
		return interfaceCount;
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if (getInterfaceCount(ship.getMutableStats()) >= 1) return false;
		if(ship.getNumFighterBays()>0) return false;
		if(ship.isStationModule()||ship.isShipWithModules()) return false;
		return true;
	}

	public void advanceInCombat(ShipAPI ship, float amount) {
		if(ship.getCurrentCR() <= 0f){
			ship.setHitpoints(0f);
		}
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if (getInterfaceCount(ship.getMutableStats()) >= 1) {
			return "Can only install 1 Custom LPC hullmod throughout your fleet and storage.";
		}
		if(ship.getNumFighterBays()>0) {
			return "Cannot be installed in ships with fighter bays.";
		}
		if(ship.isStationModule()||ship.isShipWithModules()) {
			return "Cannot be installed on modular ships or modules";
		}
		return super.getUnapplicableReason(ship);
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, final ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		tooltip.addPara("Designate this ship to be produced as a custom LPC. The ship is mothballed and cannot be used.", opad);

		if (isForModSpec || ship == null || ship.getMutableStats() == null) return;

		float dp = ship.getHullSpec().getSuppliesToRecover();

		tooltip.addPara("This ship will cause the LPC to take %s ordnance points, and set the replacement time to %s seconds.", opad, h, new String[]{Misc.getRoundedValue((int)dp*5), Misc.getRoundedValue((int)dp*5)});
		tooltip.addSectionHeading("Notes", Alignment.MID, opad);
		tooltip.addPara("If a carrier with the Custom Chip LPC installed has not enough ordnance points, the LPC is removed from the carrier and placed in cargo.", opad);


	}

}
