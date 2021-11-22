package sample;

import java.awt.geom.Point2D;
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
import robocode.RobotDeathEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class MujRobot2 extends AdvancedRobot {

	private static HashMap<String, ArrayList<Double>> q_map = new HashMap<String, ArrayList<Double>>();
	private static Double epsilon = 1.0;
	private static Double decay_rate = 0.01;
	private static Double minEpsilon = 0.01;
	private static Double alpha = 0.05;
	private static Double discount = 0.9;

	private static Integer rounds = 0;

	private static Integer reward = 0;

	private static String angle;
	private static String dist;
	private static String event = "000000";
	private static String currentState = "";
	private static String lastState = "";
	// private Integer currentAction = 0;
	private static Integer lastAction = 0;

	public void run() {
		while (true) {
			Akce();
		}

	}

	public void Akce() {
		Random randomNumber = new Random(); // WTF is this?
		int action = 0;
		double trueRandomNumber = randomNumber.nextDouble();

		// vyber nahodne akce s ohledem na pravdepodobnost epsilon
		if (epsilon > trueRandomNumber) {
			action = randomNumber.nextInt(9);
		} else {
			if (q_map.containsKey(currentState)) {
				ArrayList<Double> q_values = new ArrayList<Double>();
				q_values = q_map.get(currentState);
				Double forIndex = Collections.max(q_values);
				action = q_map.get(currentState).indexOf(forIndex);
				System.out.println(action);
			}
		}
		actions(action);
		lastState = currentState;
		currentState = getState();
		calculateQ();
		event = "000000";
	}

	public String getState() {

		return event + ":" + dist + "*" + angle + "§" + getSector();

	}

	// Definice akci
	public void actions(int action) {

		switch (action) {
		case 0:
			ahead(100);
			lastAction = 0;
			break;
		case 1:
			ahead(-100);
			lastAction = 1;
			break;
		case 2:
			turnLeft(50);
			lastAction = 2;
			break;
		case 3:
			turnRight(50);
			lastAction = 3;
			break;
		case 4:
			turnRadarLeft(120);
			lastAction = 4;
			break;
		case 5:
			turnRadarRight(120);
			lastAction = 5;
			break;
		case 6:
			fire(2);
			;
			lastAction = 6;
			break;
		case 7:
			turnGunLeft(25);
			lastAction = 7;
			break;
		case 8:
			turnGunRight(25);
			lastAction = 8;
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
		int coordX = (int) Math.floor(X / 150);
		int coordY = (int) Math.floor(Y / 150);
		return Integer.toString((coordY * 4) + coordX);
	}

	// Zvyseni poctu kol, postupny prechod na rozhodovani dle q mapy
	public void onRoundEnded(RoundEndedEvent e) {
		rounds++;
		if (rounds > 500) {
			rounds = rounds % 500;
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
		reward = reward - 15;
		event = "100000";
		Akce();
	}

	public void onBulletHit(BulletHitEvent e) {
		reward = reward + 10;
		event = "010000";
		Akce();
	}

	public void onHitRobot(HitRobotEvent e) {
		reward = reward - 50;
		event = "001000";
		Akce();
	}

	public void onBulletMissed(BulletMissedEvent e) {
		reward = reward - 10;
		event = "000100";
		Akce();
	}

	public void onHitWall(HitWallEvent e) {
		reward = reward - 50;
		event = "000010";
		Akce();
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		reward = 2;
		angle = Integer.toString((int) Math.round((e.getBearing() + 180) / 20));
		dist = Integer.toString((int) Math.round(e.getDistance() / 30));
		event = "000001";

//		************************************************************
//		*******Source: http://robowiki.net/wiki/Linear_Targeting****
		double myX = getX();
		double myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();

		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while ((++deltaTime) * (20.0 - 3.0 * 2) < Point2D.Double.distance(myX, myY, predictedX, predictedY)) {
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			if (predictedX < 18.0 || predictedY < 18.0 || predictedX > battleFieldWidth - 18.0
					|| predictedY > battleFieldHeight - 18.0) {
				predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);
				predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));

		setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
//		***********************************************************
//		***********************************************************

		Akce();

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