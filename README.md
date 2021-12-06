# Q-learning with Java Robocode
- [JDK 14](https://www.oracle.com/java/technologies/javase/jdk14-archive-downloads.html) is required to run this project
- To start the simulation run `src/tanks/RobocodeRunner.java`
- The model is pretrained in the `robots/sample/RL.data/data.dat` and `robots/sample/RL.data/data_shooting.dat` files
- To run the simulation with the pretrained model uncomment `useSavedMap()` function call at `src/sample/RL.java:61`
- To see the simulation uncomment line at `src/tanks/RobocodeRunner.java:65`
- Number of rounds can be changed at `src/tanks/RobocodeRUnner.java:69`
- To train the model from scratch don't call the `useSavedMap()` function, newly trained model will be saved to
`robots/sample/RL.data` directory
