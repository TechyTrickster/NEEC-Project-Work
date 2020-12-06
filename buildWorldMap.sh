#as configured, this should take a few hours to run.
#files that matter: theGrandList.ssv, IPLocations.csv, TraceRoutes.csv, IPMasterList.txt
java generateIPs -r $1 | parallel -P 400 fping -q -C 5 2> theGrandList.ssv
cat theGrandList.ssv | ./filterForIP.sh | sed '/.*- - - - -/d' > IPMasterList.txt
cat IPMasterList.txt | parallel -P 100 ./geolocate.sh | sed '/null/d' > IPLocations.csv
cat IPMasterList.txt | parallel -P 100 ./tracingRoutes.sh > TraceRoutes.csv

Rscript scratch.R
