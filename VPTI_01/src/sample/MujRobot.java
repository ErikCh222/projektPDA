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

public class MujRobot extends AdvancedRobot {

	private static HashMap<String, ArrayList<Double>> q_map = new HashMap<String, ArrayList<Double>>();
	private static Double epsilon = 1.0;
	private static Double decay_rate = 0.005;
	private static Double minEpsilon = 0.01;
	private static Double alpha = 0.3;
	private static Double discount = 0.9;

	private static Integer rounds = 0;

	private static Integer reward = 0;

	private static String angle;
	private static String dist;
	private static String currentState = "";
	private static String lastState = "";

	// private Integer currentAction = 0;
	private static Integer lastAction = 0;

	public void run() {
		loadQMap(); // nacteni tabulky
		Random randomNumber = new Random();
		int action = 0;
		while (true) {

			double trueRandomNumber = randomNumber.nextDouble();

			// vyber nahodne akce s ohledem na pravdepodobnost epsilon
			if (epsilon > trueRandomNumber) {
				action = randomNumber.nextInt(21);
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
			currentState = getSector() + "-" + dist + "-" + angle;
			calculateQ();
			dist = "0";
			angle = "0";
		}

	}

	// Definice akci
	public void actions(int action) {

		switch (action) {
		case 0:
			fire(1);
			lastAction = 0;
			break;
		case 1:
			ahead(100);
			lastAction = 1;
			break;
		case 2:
			back(100);
			lastAction = 2;
			break;
		case 3:
			turnLeft(30);
			lastAction = 3;
			break;
		case 4:
			turnRight(30);
			lastAction = 4;
			break;
		case 5:
			turnLeft(45);
			lastAction = 5;
			break;
		case 6:
			turnRight(45);
			lastAction = 6;
			break;
		case 7:
			turnLeft(60);
			lastAction = 7;
			break;
		case 8:
			turnRight(60);
			lastAction = 8;
			break;
		case 9:
			turnGunLeft(30);
			lastAction = 9;
			break;
		case 10:
			turnGunRight(30);
			lastAction = 10;
			break;
		case 11:
			turnGunLeft(45);
			lastAction = 11;
			break;
		case 12:
			turnGunRight(45);
			lastAction = 12;
			break;
		case 13:
			turnGunLeft(60);
			lastAction = 13;
			break;
		case 14:
			turnGunRight(60);
			lastAction = 14;
			break;
		case 15:
			turnRadarLeft(30);
			lastAction = 15;
			break;
		case 16:
			turnRadarRight(30);
			lastAction = 16;
			break;
		case 17:
			turnRadarLeft(45);
			lastAction = 17;
			break;
		case 18:
			turnRadarRight(45);
			lastAction = 18;
			break;
		case 19:
			turnRadarLeft(60);
			lastAction = 19;
			break;
		case 20:
			turnRadarRight(60);
			lastAction = 20;
			break;
		default:
			break;
		}
		execute();
	}

	// Ziskani sektoru
	public String getSector() {
		double X = getX();
		double Y = getY();
		int coordX = (int) Math.floor(X / 30);
		int coordY = (int) Math.floor(Y / 30);
		System.out.println((coordY * 20) + coordX);
		return Integer.toString((coordY * 20) + coordX);
	}

	// Zvyseni poctu kol, postupny prechod na rozhodovani dle q mapy
	public void onRoundEnded(RoundEndedEvent e) {
		rounds++;
		if (rounds > 1000) {
			rounds = rounds % 1000;
			epsilon -= decay_rate;
			if (epsilon < minEpsilon) {
				epsilon = minEpsilon;
			}
		}
	}

	public void calculateQ() {
		if (q_map.containsKey(currentState) == false || q_map.containsKey(lastState) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			tmp.add(0.0);
			q_map.put(lastState, tmp);
		}

		else {
			double q = q_map.get(lastState).get(lastAction);
			double maxQ = Collections.max(q_map.get(currentState));
			double newQ = updateQ(q, maxQ);
			reward = 0;
			ArrayList<Double> last_q_values = q_map.get(lastState);
			last_q_values.set(lastAction, newQ);
			q_map.put(lastState, last_q_values);
		}
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
	}

	public void onBulletHit(BulletHitEvent e) {
		reward = reward + 50;
	}

	public void onHitRobot(HitRobotEvent e) {
		reward = reward - 50;
	}

	public void onBulletMissed(BulletMissedEvent e) {
		reward = reward - 50;
	}

	public void onDeath(DeathEvent e) {
		reward = reward - 1000;
	}

	public void onHitWall(HitWallEvent e) {
		reward = reward - 50;
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		reward = 1;
		angle = Integer.toString((int) Math.round((e.getBearing() + 180) / 10));
		dist = Integer.toString((int) Math.round(e.getDistance() / 10));
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