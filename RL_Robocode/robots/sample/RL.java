package sample;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.SkippedTurnEvent;
import robocode.WinEvent;

public class RL extends AdvancedRobot {

	private static Double epsilon = 1.0;
	private static Double decay_rate = 0.001;
	private static Double minEpsilon = 0.01;
	private static Double alpha = 0.05;
	private static Double discount = 0.9;
	private static int wictories = 0;
	private static int losses = 0;
	private Random randomNumber = new Random();
	private static Integer rounds = 0;
	private static boolean useMap = false; 
	private static double ENbear;

	// Moving table
	private static Integer reward = 0;
	private static String currentState = "";
	private static String lastState = "";
	private static Integer lastAction = 0;
	private static HashMap<String, ArrayList<Double>> q_map = new HashMap<String, ArrayList<Double>>();

	// Shooting table
	private static Integer reward_shooting = 0;
	private static String currentState_shooting = "";
	private static String lastState_shooting = "";
	private static Integer lastAction_shooting = 0;
	private static HashMap<String, ArrayList<Double>> q_map_shooting = new HashMap<String, ArrayList<Double>>();

	public void run() {
		useSavedMap(); // Comment out to learn from scratch
		if (useMap) {
			loadState(); // nacteni tabulky
		}
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		while (true) {
			runMyTank();
		}
	}
	
	private void useSavedMap() {
		epsilon = 0.01;
		useMap = true;
	}
	

	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Moving %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
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

	public void actions(int action) {
		switch (action) {
		case 0:
			setAhead(100);
			setTurnRight(-30);
			execute();
			lastAction = 0;
			break;
		case 1:
			setAhead(-100);
			setTurnRight(-30);
			execute();
			lastAction = 1;
			break;
		case 2:
			setAhead(100);
			setTurnRight(30);
			execute();
			lastAction = 2;
			break;
		case 3:
			setAhead(-100);
			setTurnRight(30);
			execute();
			lastAction = 3;
			break;
		default:
			break;
		}
		setTurnRadarRight(-7);
	}

	public String getState() {
		int cordX = (int) Math.floor(getX() / 100);
		int cordY = (int) Math.floor(getY() / 100);
		String heading = Integer.toString((int) Math.floor(getHeading() / 20));
		String state = cordX + "*" + cordY + "-" + heading;
		return state;
	}

	public void calculateQ() {
		if (q_map.containsKey(lastState) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			for (int i = 0; i < 4; i++) {
				tmp.add(0.0);
			}
			q_map.put(currentState, tmp);
		}
		if (q_map.containsKey(currentState) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			for (int i = 0; i < 4; i++) {
				tmp.add(0.0);
			}
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

	public double updateQ(double q, double maxQ) {
		return (1 - alpha) * q + alpha * (reward + discount * maxQ);
	}	

	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Shooting %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void tankShooting() {
		int action = 0;
		double trueRandomNumber = randomNumber.nextDouble();

		// vyber nahodne akce s ohledem na pravdepodobnost epsilon
		if (epsilon > trueRandomNumber) {
			action = randomNumber.nextInt(3);
		}
		// vyber akce z Q mapy
		else if (q_map_shooting.containsKey(currentState_shooting)) {
			ArrayList<Double> q_values = new ArrayList<Double>();
			q_values = q_map_shooting.get(currentState_shooting);
			Double forIndex = Collections.max(q_values);
			action = q_map_shooting.get(currentState_shooting).indexOf(forIndex);
		}
		actionsShooting(action);

		lastState_shooting = currentState_shooting;

		// tady je reprezentace stavu
		currentState_shooting = getStateShooting();
		calculateQShooting();
	}

	public void actionsShooting(int action) {

		setTurnRadarRight(normalizeBearing(getHeading() - getRadarHeading() + ENbear));
		execute();
		
		double normalizedBearing = normalizeBearing(getHeading() - getGunHeading() + ENbear);
		switch (action) {
		case 0:
			if (normalizedBearing < 5 && normalizedBearing > -5 && getGunHeat() == 0) {
				reward_shooting = 500;
			} else {
				reward_shooting = -200;
			}
			setFire(3);
			lastAction_shooting = 0;
			break;
		case 1:
			reward_shooting = -5;
			doNothing();
			lastAction_shooting = 1;
			break;
		case 2:
			reward_shooting = 100;
			setTurnGunRight(normalizedBearing);
			lastAction_shooting = 2;
			break;
		default:
			break;
		}
		execute();
	}

	public String getStateShooting() {
		boolean gunReady;
		if (getGunHeat() == 0) {
			gunReady = true;
		} else {
			gunReady = false;
		}

		String state = (int) Math.round((normalizeBearing(ENbear + getHeading() - getGunHeading()))) + "*" + gunReady;
		return state;

	}

	public void calculateQShooting() {
		if (q_map_shooting.containsKey(lastState_shooting) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			for (int i = 0; i < 3; i++) {
				tmp.add(0.0);
			}
			q_map_shooting.put(lastState_shooting, tmp);
		}
		if (q_map_shooting.containsKey(currentState_shooting) == false) {
			ArrayList<Double> tmp = new ArrayList<Double>();
			for (int i = 0; i < 3; i++) {
				tmp.add(0.0);
			}
			q_map_shooting.put(currentState_shooting, tmp);
		}
		double q = q_map_shooting.get(lastState_shooting).get(lastAction_shooting);
		double maxQ = Collections.max(q_map_shooting.get(currentState_shooting));
		double newQ = updateQShooting(q, maxQ);
		reward_shooting = 0;
		ArrayList<Double> last_q_values = q_map_shooting.get(lastState_shooting);
		last_q_values.set(lastAction_shooting, newQ);
		q_map_shooting.put(lastState_shooting, last_q_values);
	}
	
	public double updateQShooting(double q, double maxQ) {
		return (1 - alpha) * q + alpha * (reward_shooting + discount * maxQ);
	}
	
	private double normalizeBearing(double angle) {
		while (angle > 180)
			angle -= 360;
		while (angle < -180)
			angle += 360;
		return angle;
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		ENbear = e.getBearing();
		tankShooting();
	}
	
	// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Ostatne %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

	// Zvyseni poctu kol, postupny prechod na rozhodovani dle q mapy
	public void onRoundEnded(RoundEndedEvent e) {
		rounds++;
		if (rounds > 400) {
			rounds = rounds % 400;
			epsilon -= decay_rate;
			if (epsilon < minEpsilon) {
				epsilon = minEpsilon;
			}
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		reward = -50;
		runMyTank();
	}

	public void onHitWall(HitWallEvent e) {
		reward = -3000;
	}

	public void onWin(WinEvent e) {
		wictories++;
	}

	public void onDeath(DeathEvent e) {
		losses++;
	}

	public void onSkippedTurn(SkippedTurnEvent e) {
		losses++;
	}

	public void onBattleEnded(BattleEndedEvent e) {
		try {
			saveState();
		} catch (IOException e1) {
			e1.printStackTrace(out);
		}
	}

	// %%%%%%%%%%%%%%%%%%%% Ukladanie & Nacitavanie %%%%%%%%%%%%%%%%%%%%%

	public void saveState() throws IOException {
		LocalDateTime myDateObj = LocalDateTime.now();
		DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");
		String formattedDate = myDateObj.format(myFormatObj);
		double winRate = (wictories / (double) (losses + wictories)) * 100;

		writeData(formatQTable(q_map), formattedDate + "_data.dat");
		writeData(formatQTable(q_map_shooting), formattedDate + "_data_shooting.dat");
		writeData("Wins: " + wictories + "\nLosses: " + losses + "\nWin Rate: " + winRate,
				formattedDate + "_score.txt");
	}

	private String formatQTable(HashMap<String, ArrayList<Double>> map) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, ArrayList<Double>> entry : map.entrySet()) {
			String values = String.join(",",
					entry.getValue().stream().map(op -> op.toString()).collect(Collectors.toList()));
			sb.append(entry.getKey() + "," + values + "\n");
		}
		return sb.toString();
	}

	private void writeData(String data, String fileName) {
		PrintStream printer = null;
		try {
			printer = new PrintStream(new RobocodeFileOutputStream(getDataFile(fileName)));
			printer.append(data);
		} catch (IOException e) {
			e.printStackTrace(out);
		} finally {
			printer.close();
		}
	}

	public void loadState() {
		loadTable("data.dat", q_map);
		loadTable("data_shooting.dat", q_map_shooting);
	}

	private void loadTable(String name, HashMap<String, ArrayList<Double>> map) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(getDataFile(name)));
			List<String> readLine;
			String temp;
			while (reader.ready()) {
				temp = reader.readLine();
				readLine = Arrays.asList(temp.split(","));
				ArrayList<Double> q_values = new ArrayList<Double>();
				for (int i = 1; i < readLine.size(); i++) {
					q_values.add(Double.valueOf(readLine.get(i)));
				}
				map.put(readLine.get(0), q_values);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace(out);
		}
	}
}