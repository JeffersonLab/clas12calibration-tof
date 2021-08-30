# clas12calibration-tof

FTOF and CTOF calibration suites - installation and running
===========================================================

After pulling or cloning the git repository, the suites can be installed and run as follows.

From the repository directory (clas12calibration-tof):

```
mvn install
```

This will create two jar files in the `target` directory: TOFCalibration-jar-with-dependencies.jar (FTOF suite) and CTOFCalibration-jar-with-dependencies.jar (CTOF suite)

To run, `cd target` (or move the jars to your calibration directory) then
`java -jar TOFCalibration-jar-with-dependencies.jar` for FTOF
`java -jar CTOFCalibration-jar-with-dependencies.jar` for CTOF
