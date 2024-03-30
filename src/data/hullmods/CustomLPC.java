package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;


import java.util.LinkedHashSet;
import java.util.List;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;


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
		stats.getPeakCRDuration().modifyMult(id, 0f);
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

		for (FleetMemberAPI member : fleetList) {
			if (member.getVariant().hasHullMod("customlpc")) interfaceCount++;
			LinkedHashSet<String> sMods = member.getVariant().getSMods();
			if (sMods.size() == 0) continue;
			for (String mod : sMods) {
				if (mod.equals("customlpc")) interfaceCount--;
			}
		}
		return interfaceCount;
	}

	public boolean isApplicableToShip(ShipAPI ship) {
		if(ship.getHullSize() != HullSize.FRIGATE) return false;
		if (getInterfaceCount(ship.getMutableStats()) >= 1) return false;
		if(ship.getNumFighterBays()>0) return false;
		if(ship.isStationModule()||ship.isShipWithModules()) return false;
		return true;
	}


	public void advanceInCombat(ShipAPI ship, float amount) {
		if(ship.getCurrentCR() < ship.getHitpoints()/ship.getMaxHitpoints()){
			ship.setHitpoints(ship.getCurrentCR()*ship.getMaxHitpoints());
		}
	}

	@Override
	public String getUnapplicableReason(ShipAPI ship) {
		if(ship.getHullSize() != HullSize.FRIGATE) {
			return "Can only be installed on frigate class ships.";
		}
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
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, final ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		tooltip.addPara("Designate this ship to be produced as a custom LPC. The ship is mothballed and cannot be used.", pad);

		if (isForModSpec || ship == null || ship.getMutableStats() == null) return;

		//tooltip.addPara("Once applied, the ship can be stored anywhere and can still be used using the LPC.", h, opad);

		float dp = ship.getHullSpec().getSuppliesToRecover();

		if(dp > 5){
			tooltip.addPara("This ship is too big to produce flawlessly. CR is decreased and The following d-mods are applied:", bad, pad);
			tooltip.addPara("Glitched Sensors", bad, pad);
		}
		if(dp > 6)tooltip.addPara("Degraded Engines", bad, pad);
		if(dp > 7)tooltip.addPara("Faulty Power Grid", bad, pad);
		if(dp > 8)tooltip.addPara("Compromised Structure", bad, pad);
		if(dp > 9)tooltip.addPara("Ill-advised Modifications", bad, pad);

	}

}
