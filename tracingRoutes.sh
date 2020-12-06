function assignLocation
{
	#echo "$1"
	IP="$(echo "$1" | ./filterForIP.sh)"
	latency=$(echo "$1" | cut -f 2 -d ' ')
	if [ "$latency" != "$1" ]
	then
		#echo ":NAK,null,null,NAN" | tr -d '\n'
		#don't print anything at all, its not useable anyway
	#else
		location="$(./geolocate.sh $IP | tr -d '\n')"
		echo ":$location,$latency" | tr -d '\n'
	fi
}


export -f assignLocation


echo "$(./geolocate.sh $1),STARTNODE" | tr -d '\n'
data="$(traceroute -F -n -q 1 $1 | sed '1d' | sed -E 's/\s+/ /g' | sed 's/ ms/:/g' | sed 's/\*/\*:/g' | sed 's/^\s*//g' | cut -f 2- -d ' ' | tr -d '\n')"
#echo "$data" | tr ':' '\n' | parallel -P 1 assignLocation
echo ""
#echo "$data" | sed 's/:$//g'
