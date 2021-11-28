package sample;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

import robocode.AdvancedRobot;
import robocode.BattleEndedEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobocodeFileOutputStream;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class MujRobot extends AdvancedRobot {

	private static HashMap<String, ArrayList<Double>> q_map = new HashMap<String, ArrayList<Double>>();
	private static Double epsilon = 0.5;
	private static Double decay_rate = 0.005;
	private static Double minEpsilon = 0.01;
	private static Double alpha = 0.3;
	private static Double discount = 0.9;
	private static Integer rounds = 0;
	private static Integer reward = 0;
	private static Double angle = 0.0;
	private static String dist;
	private static String currentState = "";
	private static String lastState = "";
	private static Integer wictories = 0;
	private static Integer losses = 0;

	// private Integer currentAction = 0;
	private static Integer lastAction = 0;
	public Random randomNumber = new Random();
	private static boolean useMap = false;

	private static HashMap<Integer, RobotFunction> mapOfActions = new HashMap<Integer, RobotFunction>();
	
	
	public void run() {
		if (useMap) {
			loadQMap(); // nacteni tabulky
			epsilon = 0.9;
		}
		initializeMapOfActions();
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
			action = randomNumber.nextInt(mapOfActionSize);
		}
		// vyber akce z Q mapy
		else if (q_map.containsKey(currentState)) {
			ArrayList<Double> q_values = new ArrayList<Double>();
			q_values = q_map.get(currentState);
			Double forIndex = Collections.max(q_values);
			action = q_map.get(currentState).indexOf(forIndex);
		}
		
		RobotFunction rf = this.getMapOfActions().get(action);
		
		actions(rf);
		lastAction = action;
		lastState = currentState;

		// tady je reprezentace stavu
		currentState = getState();
		
		calculateQ();

	}

	
	public void initializeMapOfActions() {
		List<String> actionNames = Arrays.asList(
						"setTurnRight", "setTurnLeft",
						"setAhead", "setBack",
						"setFire",
						"setTurnGunLeft","setTurnGunRight"
						);
		int mapVals= -1;
		for(String name: actionNames){
			if(name == "setFire") {
				for (int i = 0; i<20; i++) {
					mapVals++;
					this.getMapOfActions().put(mapVals, new RobotFunction(i*5, name));
				}
			}
			else if(name == "setAhead" || name == "setBack") {
				for (int j = 1; j<100; j+=10 ) {
					mapVals++;
					this.getMapOfActions().put(mapVals, new RobotFunction(j, name));
				}
				
				
			}
			else {
				for (int j = 1; j<360 ; j+=10 ) {
					mapVals++;
					this.getMapOfActions().put(mapVals, new RobotFunction(j, name));
				}
			}
		}
		mapOfActionSize = this.getMapOfActions().size();
	}
	
	
	public void actions(RobotFunction robotFunction) {

		switch (robotFunction.getFunctionName()) {

		case "setAhead":
			setAhead(robotFunction.getParameter());
			break;
		case "setBack":
			setBack(robotFunction.getParameter());
			break;
		case "setTurnRight":
			setTurnRight(robotFunction.getParameter());
			break;
		case "setTurnLeft":
			setTurnLeft(robotFunction.getParameter());
			break;
		case "setFire":
			setFire(robotFunction.getParameter());
			break;
		case "setTurnGunLeft":
			setTurnGunLeft(robotFunction.getParameter());
			break;
		case "setTurnGunRight":
			setTurnGunRight(robotFunction.getParameter());
			break;
		default:
			doNothing();
			break;
		}
		execute();
	}

	// Ziskani sektoru
	public String getState() {
		int energy;
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

		return energy + "-" + heat + "-";
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
		for(int i=0; i<this.mapOfActionSize;i++) {
			tmp.add(0.0);
		}
		q_map.put(state, tmp);
	}

	public double updateQ(double q, double maxQ) {
		return (1 - alpha) * q + alpha * (reward + discount * maxQ);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		reward = reward - 40;
		runMyTank();
	}

	public void onBulletHit(BulletHitEvent e) {
		reward = reward + 300;

	}

	public void onHitRobot(HitRobotEvent e) {
		reward = reward - 10;
		runMyTank();
	}

	public void onBulletMissed(BulletMissedEvent e) {
		reward = reward - 40;

	}

	public void onDeath(DeathEvent e) {
		reward = (int) (reward - (100000/e.getTime()));
		out.println("Time alive" + e.getTime() );
		losses++;
		
	}

	public void onWin(WinEvent e) {
		reward = reward + 1000;
		wictories++;
	}
	
	public void onRobotDeath(RobotDeathEvent e) {
		reward = reward + 300;
	}

	public void onHitWall(HitWallEvent e) {
		reward = reward - 100;
		setAhead(getVelocity() * -1);
		runMyTank();
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		reward = 50;
		angle = e.getBearing();
		dist = Integer.toString((int) Math.round(e.getDistance() / 10));
		fire(3);
		scan();
		runMyTank();
	}

	public void saveState() throws IOException {
		LocalDateTime myDateObj = LocalDateTime.now();
		DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");
		String formattedDate = myDateObj.format(myFormatObj);

		StringBuilder sb = new StringBuilder();
		for (Entry<String, ArrayList<Double>> entry : q_map.entrySet()) {
			String values = String.join(",",
					entry.getValue().stream().map(op -> op.toString()).collect(Collectors.toList()));
			sb.append(entry.getKey() + "," + values + "\n");
		}
		writeData(sb.toString(), formattedDate + "_data.dat");

		double winRate = (wictories / (double) losses) * 100;
		writeData("Wins: " + wictories + "\nLosses: " + losses + "\nWin Rate: " + winRate,
				formattedDate + "_score.txt");

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

		try {
			BufferedReader reader = new BufferedReader(new FileReader(getDataFile("data.dat")));
			List<String> readLine;
			String temp;
			while (reader.ready()) {
				temp = reader.readLine();
				readLine = Arrays.asList(temp.split(","));
				ArrayList<Double> q_values = new ArrayList<Double>();
				for (int i = 1; i < readLine.size(); i++) {
					q_values.add(Double.valueOf(readLine.get(i)));
				}
				q_map.put(readLine.get(0), q_values);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace(out);
		}
	}

	public void onBattleEnded(BattleEndedEvent e) {
		try {
			saveState();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public HashMap<Integer, RobotFunction> getMapOfActions() {
		return mapOfActions;
	}

	public void setMapOfActions(HashMap<Integer, RobotFunction> mapOfActions) {
		MujRobot.mapOfActions = mapOfActions;
	}

}