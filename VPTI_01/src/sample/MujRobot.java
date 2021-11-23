package sample;

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
	private static Double epsilon = 1.0;
	private static Double decay_rate = 0.05;
	private static Double minEpsilon = 0.01;
	private static Double alpha = 0.3;
	private static Double discount = 0.9;

	private static Integer rounds = 0;

	private static Integer reward = 0;

	private static Double angle = 0.0;
	private static String dist;
	private static String currentState = "";
	private static String lastState = "";

	// private Integer currentAction = 0;
	private static Integer lastAction = 0;
	public Random randomNumber = new Random();
	private static boolean useMap = true;

	public void run() {
		if (useMap) {
			loadQMap(); // nacteni tabulky
			epsilon = 0.01;
		}
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
			action = randomNumber.nextInt(8);
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
		if (!useMap) {
			calculateQ();
		}
		dist = "0";

	}

	public void actions(int action) {

		switch (action) {

		case 0:
			setAhead(100);
			turnRight(40);
			lastAction = 0;
			break;
		case 1:
			setAhead(100);
			turnRight(-40);
			lastAction = 1;
			break;
		case 2:
			setAhead(-100);
			turnRight(40);
			lastAction = 2;
			break;
		case 3:
			setAhead(100);
			turnRight(-40);
			lastAction = 3;
			break;
		case 4:
			// turnRadarRight(120);
			setTurnRight(angle + 10);
			setAhead(100);
			lastAction = 4;
			break;
		case 5:
			setTurnRight(angle);
			setAhead(100);
			fire(2);
			lastAction = 5;
			break;
		case 6:
			// turnGunRight(-20);
			// turnRadarRight(-20);
			doNothing();
			lastAction = 6;
			break;

		/*
		 * case 0: fire(1); lastAction = 0; break; case 1: ahead(100); lastAction = 1;
		 * break; case 2: back(100); lastAction = 2; break; case 3: turnLeft(30);
		 * lastAction = 3; break; case 4: turnRight(30); lastAction = 4; break; case 5:
		 * turnLeft(45); lastAction = 5; break; case 6: turnRight(45); lastAction = 6;
		 * break; case 7: turnGunLeft(30); lastAction = 7; break; case 8:
		 * turnGunRight(30); lastAction = 8; break; case 9: turnGunLeft(45); lastAction
		 * = 9; break; case 10: turnGunRight(45); lastAction = 10; break; case 11:
		 * turnRadarLeft(30); lastAction = 11; break; case 12: turnRadarRight(30);
		 * lastAction = 12; break; case 13: turnRadarLeft(45); lastAction = 13; break;
		 * case 14: turnRadarRight(45); lastAction = 14; break;
		 */
		default:
			break;
		}
		execute();
	}

	// Ziskani sektoru
	public String getState() {
		int energy;
		/*
		 * double X = getX(); double Y = getY(); int coordX = (int) Math.floor(X / 30);
		 * int coordY = (int) Math.floor(Y / 30); System.out.println((coordY * 20) +
		 * coordX);
		 */
		if (getEnergy() < 25) {
			energy = 0;
		} else if (getEnergy() > 25 && getEnergy() < 50) {
			energy = 1;
		} else if (getEnergy() > 50 && getEnergy() < 75) {
			energy = 2;
		} else {
			energy = 3;
		}

		int heat;
		if (getGunHeat() > 0) {
			heat = 0;
		} else {
			heat = 1;
		}

		return energy + "-" + heat + "-" + dist;
		// + "-"+angle;
	}

	// Zvyseni poctu kol, postupny prechod na rozhodovani dle q mapy
	public void onRoundEnded(RoundEndedEvent e) {
		rounds++;
		if (rounds > 50) {
			rounds = rounds % 50;
			epsilon -= decay_rate;
			if (epsilon < minEpsilon) {
				epsilon = minEpsilon;
			}
		}
	}

	public void calculateQ() {
        if (q_map.containsKey(lastState) == false) {
        	createStateMap(lastState);
        }
        if (q_map.containsKey(currentState) == false) {
        	createStateMap(currentState);
        }

            double q = q_map.get(lastState).get(lastAction);
            double maxQ = Collections.max(q_map.get(currentState));
            double newQ = updateQ(q, maxQ);
            reward = 0;
            ArrayList<Double> last_q_values = q_map.get(lastState);
            last_q_values.set(lastAction, newQ);
            q_map.put(lastState, last_q_values);
        
        
    }

	public void createStateMap(String state) {
		ArrayList<Double> tmp = new ArrayList<Double>();
		tmp.add(0.0);
		tmp.add(0.0);
		tmp.add(0.0);
		tmp.add(0.0);
		tmp.add(0.0);
		tmp.add(0.0);
		tmp.add(0.0);
		q_map.put(state, tmp);
	}

	public double updateQ(double q, double maxQ) {
		return (1 - alpha) * q + alpha * (reward + discount * maxQ);
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

	/*
	 * ODMENY -onRobotDeath -onBulletHit done -onHitByBullet done -onHitRobot done
	 * -onBulletMissed done -onDeath done -onHitWall done -onScannedRobot
	 * -getHeading -getDistance -getBearing
	 */

	public void onHitByBullet(HitByBulletEvent e) {
		reward = reward - 50;
		runMyTank();
	}

	public void onBulletHit(BulletHitEvent e) {
		reward = reward + 80;

	}

	public void onHitRobot(HitRobotEvent e) {
		reward = reward - 50;
		runMyTank();
	}

	public void onBulletMissed(BulletMissedEvent e) {
		reward = reward - 50;

	}

	public void onDeath(DeathEvent e) {
		reward = reward - 50;

	}

	public void onWin(WinEvent e) {
		reward = reward + 100;

	}

	public void onHitWall(HitWallEvent e) {
		reward = reward - 50;
		setAhead(getVelocity() * -1);
		runMyTank();
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		reward = 1;
		// angle = Integer.toString((int) Math.round((e.getBearing() + 180) / 10));
		angle = e.getBearing();
		dist = Integer.toString((int) Math.round(e.getDistance() / 10));
		fire(3);
		scan();
		runMyTank();
	}

	public void onBattleEnded(BattleEndedEvent e) {
		try {
			saveQMap();
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

}