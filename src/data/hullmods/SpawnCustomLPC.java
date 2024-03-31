package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import java.util.List;
import java.util.Objects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
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
			ship.setAlphaMult(0);
			ship.setCollisionClass(CollisionClass.NONE);
			ship.setShipAI(null);
			ship.setDrone(true);
			ship.setHullSize(HullSize.FIGHTER);
			ship.getLocation().set(1000000f, 0f);



			CombatEngineAPI engine = Global.getCombatEngine();
			CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
			List<FleetMemberAPI> fleetList = fleet.getMembersWithFightersCopy();
			ShipAPI mothership = ship.getWing().getSourceShip();
			
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
				ship.setShipAI(null);
				ship.setDrone(true);
				ship.setHullSize(HullSize.FIGHTER);
				ship.getLocation().set(1000000f, 0f);

				if(customlpccopy != null){
					engine.getFleetManager(ship.getOwner()).setSuppressDeploymentMessages(true);
					customlpc = spawnShipOrWingDirectly(customlpccopy, FleetMemberType.SHIP, ship.getOwner(),mothership.getCurrentCR(),mothership.getLocation(),mothership.getFacing());
					engine.getFleetManager(ship.getOwner()).setSuppressDeploymentMessages(false);
					customlpc.setCollisionClass(CollisionClass.FIGHTER);
					customlpc.setLaunchingShip(mothership);
					customlpc.setAnimatedLaunch();
					customlpc.setDrone(true);
					customlpc.setCaptain(ship.getCaptain());
				}
			} else {
				if(customlpc != null){
					if(!customlpc.isAlive() || !ship.getWing().getSourceShip().isAlive()){
						ship.setHitpoints(0);
						engine.removeEntity(ship);
					}
				}
				if(!ship.isAlive()){
					customlpc.setHitpoints(0);
					engine.removeEntity(customlpc);
				}
			}
		}

	}

	@Override
	public boolean affectsOPCosts() {
		return false;
	}

	public static ShipAPI spawnShipOrWingDirectly(ShipVariantAPI variant, FleetMemberType type, int owner, float combatReadiness, Vector2f location, float facing) {

		FleetMemberAPI member = Global.getFactory().createFleetMember(type, variant);
		member.setOwner(owner);
		member.getCrewComposition().addCrew(member.getNeededCrew());
		ShipAPI ship = Global.getCombatEngine().getFleetManager(owner).spawnFleetMember(member, location, facing, 0f);

		ship.setCRAtDeployment(combatReadiness);
		ship.setCurrentCR(combatReadiness);
		ship.setOwner(owner);
		ship.getShipAI().forceCircumstanceEvaluation();
		return ship;
	}


}

