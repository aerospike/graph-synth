Prerequisites:
  you must install git, maven, and a jdk

To build the project, start with your working directory in the root of the git repository.

```bash
~/graph-synth$ mvn -DskipTests clean package
...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for GraphSynth 1.0.0-SNAPSHOT:
[INFO] 
[INFO] GraphSynth ......................................... SUCCESS [  0.229 s]
[INFO] generator .......................................... SUCCESS [  7.747 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.109 s
[INFO] Finished at: 2023-10-30T23:59:10-06:00
[INFO] ------------------------------------------------------------------------
```
