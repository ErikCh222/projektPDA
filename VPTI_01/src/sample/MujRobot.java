package sample;

/*nahradit aby kdyz se otaci neotacel scanerem*/
/*boolean promenna do onEvent˘ pokud fire - nechat dolÌtnout kulku a pak udÏlat dalöÌ akci */








// calculate Q nevolat za strelbu 2x, aù to nep¯ipÌöe jin˝m akcÌm 

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.*;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.Robot;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class MujRobot extends AdvancedRobot {

	private static HashMap<String, ArrayList<Double>> q_map = new HashMap<String, ArrayList<Double>>();
	private static HashMap<String, ArrayList<Double>> q_map_strelba = new HashMap<String, ArrayList<Double>>();
	private static Double epsilon = 1.0; ////zpatky zmenit !!!!!!!!!!!!!!!!
	private static Double decay_rate = 0.01;
	private static Double minEpsilon = 0.01;
	private static Double alpha = 0.1;
	private static Double discount = 0.9;

	private static Integer rounds = 0;

	private static Integer reward = 0;
	private static Integer reward_strelba = 0;
	
	private static boolean useMap = true;
	private static String angle = "";
	private static String dist = "";
	private static String vel = "";
	private static String currentState = "";
	private static String currentState_strelba = "";
	private static String frozenState = "";
	private static String lastState = "";
	private static String lastState_strelba = "";
	private static Integer lastAction_strelba = 0;
	public static String onHitBull="";

	// private Integer currentAction = 0;
	private static Integer lastAction = 0;
	public Random randomNumber = new Random();
	public void run() {
		 if (useMap) {
	            loadQMap(); // nacteni tabulky
	            loadQMap_strelba();
	            epsilon = 0.01;
	            useMap = false;
	        }
	//	loadQMap(); // nacteni tabulky
		
		
		while (true) {
			runMyTank();
		}

	}

	// Definice akci
	
	public void runMyTank() {
		int action = 0;
		double trueRandomNumber = randomNumber.nextDouble();

		// vyber nahodne akce s ohledem na pravdepodobnost epsilon
		if (epsilon > trueRandomNumber) {
			action = randomNumber.nextInt(4);
		}
		// vyber akce z Q mapy
		else if (q_map.containsKey(currentState)) {
			ArrayList<Double> q_values = new ArrayList<Double>();
			q_values = q_map.get(currentState);
			Double forIndex = Collections.max(q_values);
			action = q_map.get(currentState).indexOf(forIndex);
		}
		actions(action);
		
		lastState = currentState;

		// tady je reprezentace stavu
		currentState = getState();
		calculateQ();
	}
	
	
	public void Tank_strelba() {
		int action = 0;
		double trueRandomNumber = randomNumber.nextDouble();

		// vyber nahodne akce s ohledem na pravdepodobnost epsilon
		if (epsilon > trueRandomNumber) {
			action = randomNumber.nextInt(3);
		}
		// vyber akce z Q mapy
		else if (q_map_strelba.containsKey(currentState_strelba)) {
			ArrayList<Double> q_values = new ArrayList<Double>();
			q_values = q_map_strelba.get(currentState_strelba);
			Double forIndex = Collections.max(q_values);
			action = q_map_strelba.get(currentState_strelba).indexOf(forIndex);
		}
		actions_strelba(action);
		
		lastState_strelba = currentState_strelba;

		// tady je reprezentace stavu
		currentState_strelba = getState_strelba();
		calculateQ_strelba();
	}
	
	public void actions(int action) {

		switch (action) {
		
		case 0:
			if (onHitBull=="1") {
				reward = 0;
			}
			setAhead(100);
			setTurnLeft(30);
			setAdjustGunForRobotTurn(true);
			setTurnGunLeft(7);
			execute();
			lastAction = 0;
			onHitBull = "0";
			break;
		case 1:
			if (onHitBull=="1") {
				reward = 0;
			}
			setBack(100);
			setTurnLeft(30);
			setAdjustGunForRobotTurn(true);
			setTurnGunLeft(7);	
			execute();
			lastAction = 1;	
			onHitBull = "0";
			break;
		case 2:
			if (onHitBull=="1") {
				reward = 0;
			}
			setAhead(100);
			setTurnLeft(30);
			setAdjustGunForRobotTurn(true);
			setTurnGunLeft(7);
			execute();
			lastAction = 2;
			onHitBull = "0";
			break;
		case 3:
			if (onHitBull=="1") {
				reward = 0;
			}
			setBack(100);
			setTurnRight(30);
			setAdjustGunForRobotTurn(true);
			setTurnGunLeft(7);
			execute();
			lastAction = 3;
			onHitBull = "0";
			break;

		default:
			break;
		}
	}
	
	public void actions_strelba(int action) {

		execute();
		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10)
		switch (action) {
		
		case 2:
			turnGunRight(3);
			lastAction_strelba = 2;
			break;
		case 1:
			turnGunLeft(3);
			lastAction_strelba = 1;			
			break;
		case 0:
			setFire(3);
			lastAction_strelba = 0;
			frozenState=getState_strelba();
			break;

		default:
			break;
		}
		
	}

	// Ziskani sektoru
	public String getState_strelba() {

		String state =  dist + "-" + angle + "*" + vel;
		
		return state;
	}
	
	public String getState() {
		int cordX = (int)Math.round(getX()/100);
		int cordY = (int)Math.round(getY()/100);
		String sector = Integer.toString(cordY*6+cordX);
		String heading = Integer.toString((int)Math.round(getHeading()/36));
		
		
		String state =  sector + "-" + heading + "-" + onHitBull;
		
		return state;
	}

	// Zvyseni poctu kol, postupny prechod na rozhodovani dle q mapy
	public void onRoundEnded(RoundEndedEvent e) {
		rounds++;
		if (rounds > 300) {
			rounds = rounds % 300;
			epsilon -= decay_rate;
			if (epsilon < minEpsilon) {
				epsilon = minEpsilon;
			}
		}
	}

	public void calculateQ() {
		if (q_map.containsKey(lastState) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			q_map.put(lastState, tmp);
		}
		if (q_map.containsKey(currentState) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			q_map.put(currentState, tmp);
		}

			double q = q_map.get(lastState).get(lastAction);
			double maxQ = Collections.max(q_map.get(currentState));
			double newQ = updateQ(q, maxQ);
			reward = 0;
			ArrayList<Double> last_q_values = q_map.get(lastState);
			last_q_values.set(lastAction, newQ);
			q_map.put(lastState, last_q_values);
		
		
	}
	
	public void calculateQ_strelba() {
		if (q_map_strelba.containsKey(lastState_strelba) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			q_map_strelba.put(lastState_strelba, tmp);
		}
		if (q_map_strelba.containsKey(currentState_strelba) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			q_map_strelba.put(currentState_strelba, tmp);
		}

			double q = q_map_strelba.get(lastState_strelba).get(lastAction_strelba);
			double maxQ = Collections.max(q_map_strelba.get(currentState_strelba));
			double newQ = updateQ_strelba(q, maxQ);
			reward_strelba = 0;
			ArrayList<Double> last_q_values = q_map_strelba.get(lastState_strelba);
			last_q_values.set(lastAction_strelba, newQ);
			q_map_strelba.put(lastState_strelba, last_q_values);
		
		
	}

	public double updateQ(double q, double maxQ) {
		return (1 - alpha) * q + alpha * (reward + discount * maxQ);
	}
	
	public double updateQ_strelba(double q, double maxQ) {
		return (1 - alpha) * q + alpha * (reward_strelba + discount * maxQ);
	}

	public void saveQMap() throws IOException {
		PrintStream w = null;
		try {
			w = new PrintStream(new RobocodeFileOutputStream(getDataFile("q_map.dat")));
			for (Entry<String, ArrayList<Double>> entry : q_map.entrySet()) {
				w.println(entry.getKey() + ":" + entry.getValue());
			}

			if (w.checkError()) {
				out.println("I could not write the count!");
			}
		} catch (IOException e) {
			out.println("IOException trying to write: ");
			e.printStackTrace(out);
		} finally {
			if (w != null) {
				w.close();
			}
		}
	}
	
	public void saveQMap_strelba() throws IOException {
		PrintStream w = null;
		try {
			w = new PrintStream(new RobocodeFileOutputStream(getDataFile("q_map_strelba.dat")));
			for (Entry<String, ArrayList<Double>> entry : q_map_strelba.entrySet()) {
				w.println(entry.getKey() + ":" + entry.getValue());
			}

			if (w.checkError()) {
				out.println("I could not write the count!");
			}
		} catch (IOException e) {
			out.println("IOException trying to write: ");
			e.printStackTrace(out);
		} finally {
			if (w != null) {
				w.close();
			}
		}
	}

	/*
	 * ODMENY -onRobotDeath -onBulletHit done -onHitByBullet done -onHitRobot done
	 * -onBulletMissed done -onDeath done -onHitWall done -onScannedRobot
	 * -getHeading -getDistance -getBearing
	 */

	public void onHitByBullet(HitByBulletEvent e) {
		reward = -50;
		onHitBull = "1";
		runMyTank();
	}

	public void onBulletHit(BulletHitEvent e) {
		reward_strelba = 400;
		

     	double q = q_map_strelba.get(frozenState).get(0)*(1-alpha);
		ArrayList<Double> last_q_values = q_map_strelba.get(frozenState);
		last_q_values.set(0, q+(alpha*reward_strelba));
		q_map_strelba.put(frozenState, last_q_values);
		reward_strelba = 0;
		
	}

	public void onBulletMissed(BulletMissedEvent e) {
		reward_strelba = -100;
		
     	double q = q_map_strelba.get(frozenState).get(0)*(1-alpha);
		ArrayList<Double> last_q_values = q_map_strelba.get(frozenState);
		last_q_values.set(0, q+(alpha*reward_strelba));
		q_map_strelba.put(frozenState, last_q_values);
		reward_strelba = 0;
		
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e) {
		// asi nepot¯ebuju ale p¯edstavuje poslednÌ stav co m˘ûe vyvolat bullet
	}

	public void onHitWall(HitWallEvent e) {
		reward = - 500;
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		setTurnGunRight(getHeading() - getGunHeading() + e.getBearing());
		reward = 5;
		angle = Integer.toString((int) Math.round((e.getHeading() + 180) / 15));
		dist = Integer.toString((int) Math.round(e.getDistance() / 20));
		vel = Integer.toString((int) Math.round(e.getVelocity()));		
		Tank_strelba();
	}

	public void onBattleEnded(BattleEndedEvent e) {
		try {
			saveQMap();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			saveQMap_strelba();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void loadQMap() {

		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(getDataFile("q_map.dat")));
				String[] line;
				String tmp = reader.readLine();
				while (tmp != null) {
					line = tmp.split(":");
					ArrayList<Double> tmpAr = new ArrayList<Double>();
					for (String s : line[1].replace("[", "").replace("]", "").split(",")) {
						tmpAr.add(Double.valueOf(s));
					}
					q_map.put(line[0], tmpAr);
					tmp = reader.readLine();
				}
				out.println("Size of map durring load -- " + q_map.size());
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		} catch (IOException e) {
		}
	}
	
	public void loadQMap_strelba() {

		try {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(getDataFile("q_map_strelba.dat")));
				String[] line;
				String tmp = reader.readLine();
				while (tmp != null) {
					line = tmp.split(":");
					ArrayList<Double> tmpAr = new ArrayList<Double>();
					for (String s : line[1].replace("[", "").replace("]", "").split(",")) {
						tmpAr.add(Double.valueOf(s));
					}
					q_map_strelba.put(line[0], tmpAr);
					tmp = reader.readLine();
				}
				out.println("Size of map durring load -- " + q_map_strelba.size());
			} finally {
				if (reader != null) {
					reader.close();
				}
			}
		} catch (IOException e) {
		}
	}

}