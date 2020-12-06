locationData="$(curl -s https://ipvigilante.com/$1)"
if [ $(echo "$locationData" | wc -w) -lt 10 ]
then
	output="$(echo $locationData | jq '.data.latitude, .data.longitude' | tr -d '\"' | tr '\n' ',' | sed 's/,$//g')"
	if [ "$output" == "" ]
	then
		echo "$1,null,null"
	else
		echo "$1,$output"
	fi
else
	echo "$1,null,null"
fi
