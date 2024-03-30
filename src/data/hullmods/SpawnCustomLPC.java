package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import java.util.List;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import org.lwjgl.util.vector.Vector2f;

@SuppressWarnings("unchecked")
public class SpawnCustomLPC extends BaseHullMod {

	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {

		return null;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new ReplaceCustomLPC(ship));
	}

	public static class ReplaceCustomLPC implements AdvanceableListener {
		protected ShipAPI ship;
		protected boolean fired = false;

		protected ShipAPI customlpc = null;

		protected ShipVariantAPI customlpccopy = null;

		public ReplaceCustomLPC(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {

			CombatEngineAPI engine = Global.getCombatEngine();
			CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
			List<FleetMemberAPI> fleetList = fleet.getMembersWithFightersCopy();
			
			if (!fired) {
				fired = true;
				for (FleetMemberAPI member : fleetList) {
					if (member.getVariant().hasHullMod("customlpc")){
						customlpccopy = member.getVariant().clone();
						break;
					}
				}
				ship.setAlphaMult(0);
				ship.setCollisionClass(CollisionClass.NONE);
				ship.setHoldFire(true);

				if(customlpccopy != null){
					engine.getFleetManager(ship.getOwner()).setSuppressDeploymentMessages(true);
					customlpc = spawnShipOrWingDirectly(customlpccopy, FleetMemberType.SHIP, ship.getOwner(),ship.getCurrentCR(),ship.getCurrentCR()*5/customlpccopy.getHullSpec().getSuppliesPerMonth(),ship.getLocation(),ship.getFacing());
					engine.getFleetManager(ship.getOwner()).setSuppressDeploymentMessages(false);
					customlpc.setCollisionClass(CollisionClass.FIGHTER);
					customlpc.setLaunchingShip(ship);
					customlpc.setAnimatedLaunch();
					customlpc.setDrone(true);
				}
			} else {
				if(customlpc != null){
					if(!customlpc.isAlive() || !ship.getWing().getSourceShip().isAlive()){
						ship.setHitpoints(0);
						engine.removeEntity(ship);
					}
				}
				if(!ship.isAlive()){
					engine.removeEntity(customlpc);
				}
			}
		}

	}

	@Override
	public boolean affectsOPCosts() {
		return false;
	}

	public static ShipAPI spawnShipOrWingDirectly(ShipVariantAPI variant, FleetMemberType type, int owner, float combatReadinessmax,float combatReadinesscurrent, Vector2f location, float facing) {

		float dp = variant.getHullSpec().getSuppliesToRecover();
		if(dp > 5)variant.addMod(HullMods.GLITCHED_SENSORS);
		if(dp > 6)variant.addMod(HullMods.DEGRADED_ENGINES);
		if(dp > 7)variant.addMod(HullMods.FAULTY_GRID);
		if(dp > 8)variant.addMod(HullMods.COMP_STRUCTURE);
		if(dp > 9)variant.addMod(HullMods.ILL_ADVISED);

		/*

		if(variant.getHullSize() == HullSize.FRIGATE){

		}else if(variant.getHullSize() == HullSize.DESTROYER) {

		}else if(variant.getHullSize() == HullSize.CRUISER){

		}else if(variant.getHullSize() == HullSize.CAPITAL_SHIP){

		}

		 */

		FleetMemberAPI member = Global.getFactory().createFleetMember(type, variant);
		member.setOwner(owner);
		member.getCrewComposition().addCrew(member.getNeededCrew());
		ShipAPI ship = Global.getCombatEngine().getFleetManager(owner).spawnFleetMember(member, location, facing, 0f);

		ship.setCRAtDeployment(combatReadinessmax);
		ship.setCurrentCR(combatReadinesscurrent);
		ship.setOwner(owner);
		ship.getShipAI().forceCircumstanceEvaluation();
		return ship;
	}


}

