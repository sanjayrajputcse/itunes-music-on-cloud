#!/bin/bash

#================== configs ==================
musicOnCloud="/Users/sanjay.rajput/personal-github-repos/itunes-music-on-cloud/target/itunes-music-on-cloud-1.0.jar"
java="/usr/bin/java"
maxSongSize=52428800 ## 50 MB
fieldDelimiter="&FIELD_DELIMETER&"
rowDelimiter="&ROW_DELIMETER&"
pageSize=50
tempSongFile="/tmp/itunesMusicInfo.txt"

function trim() {
	echo $1 | sed "s/^ *//g" | sed "s/^,*//g" | sed "s/^ *//g" | sed "s/ *$//g" | sed "s/,*$//g" | sed "s/ *$//g"
}

function parseLocation() {
	echo "$1" | sed 's/alias Macintosh HD//g' | sed 's/:/\//g'           ###| sed 's/ /\\ /g' | sed 's/(/\\(/g' | sed 's/)/\\)/g' | sed "s/\[/\\\[/g" | sed "s/\]/\\\]/g" | sed "s/{/\\{/g" | sed 's/}/\\}/g'
}

function formatFileSize() {
	len=$1
	if (( $(bc <<< "len > 1024") )) ##KB
	then
		lenD=`echo "scale=2 ; $len / 1024" | bc`
		if (( $(bc <<< "$lenD > 1024") )) ##MB
		then
			lenD=`echo "scale=2 ; $lenD / 1024" | bc`
			if (( $(bc <<< "$lenD > 1024") )) ##GB
			then
				lenD=`echo "scale=2 ; $lenD / 1024" | bc`
				echo $lenD GB
                	else
                        	echo $lenD MB
                	fi  
		else
			echo $lenD KB
		fi
	else
		echo $len B
	fi
}
echo "======================== MAC iTunes Library and Google Drive Sync Utility ========================"
echo "===== 1. Exporting local songs to google drive ====="
echo "fetching all songs from iTunes Library..."
all_lib_songs=`osascript <<EOF
tell application "iTunes"
	set max to number of tracks of playlist "Library"
	do shell script "echo " & max as string
	set output to {}
	set counter to 0
	repeat while counter < max
		set counter to counter + 1
		set output to output & "%%" & name of track counter of playlist "Library" & "%%" & artist of track counter of playlist "Library" & "%%" & album of track counter of playlist "Library" & "%%" & time of track counter of playlist "Library" & "%%" & location of track counter of playlist "Library" & "%%" & rating of track counter of playlist "Library" &  "%%" & genre of track counter of playlist "Library" & "%%" & year of track counter of playlist "Library" & "%%" &  "%%" & size of track counter of playlist "Library" & "%%" & kind of track counter of playlist "Library" & "<<>>"
	end repeat
end tell
EOF`

declare -a songs
counter=0
IFS="<<>>" read -ra ADDR <<< ",$all_lib_songs"
for i in "${ADDR[@]}"; do
	if [ -n "$i" ]; then
		songs[$counter]=$i
		counter=$((counter+1))
	fi
done
total_songs=$counter
echo "Total Songs: $total_songs"
echo "extracting individual song info..."
c=0
total_size=0
all_songs_info=""
ignored=0
> $tempSongFile
for i in "${songs[@]}"; do
	IFS='%%' read -ra ADDR <<< "$i"
	#echo "Song INFO: ${ADDR[*]}"
	#echo "1: ${ADDR[1]} 2: ${ADDR[2]} 3: ${ADDR[3]} 4: ${ADDR[4]} 5: ${ADDR[5]} 6: ${ADDR[6]} 7: ${ADDR[7]} 8: ${ADDR[8]} 9: ${ADDR[9]} 10: ${ADDR[10]} 11: ${ADDR[11]} "
	name=$(trim "${ADDR[2]}")
	artist=$(trim "${ADDR[4]}")
	album=$(trim "${ADDR[6]}")
	time=$(trim "${ADDR[8]}")
	location=$(parseLocation "${ADDR[10]}")
	fileName=`echo ${location##*/}`
	rating=$(trim "${ADDR[12]}")
	genre=$(trim "${ADDR[14]}")
	year=$(trim "${ADDR[16]}")
	size=$(trim "${ADDR[20]}")
	kind=$(trim "${ADDR[22]}")
	fSize=$(formatFileSize "$size")
	#echo "$c. Name: $name, Artist: $artist, Album: $album, Time: $time, Location: $location, rating: $rating, genre: $genre, year: $year, size: $fSize, kind: $kind"
	c=$((c+1))
	if [ "$fileName" != "missing value" ] && [ "$location" != "missing value" ] && [[ ${size} != *"E+"* ]] && (( $(bc <<< "$size < $maxSongSize") )); then
		total_size=$(bc <<< "$total_size + $size")
		current_song_info="$fileName$fieldDelimiter$location$fieldDelimiter$name$fieldDelimiter$artist$fieldDelimiter$album$fieldDelimiter$time$fieldDelimiter$rating$fieldDelimiter$genre$fieldDelimiter$year$fieldDelimiter$size$fieldDelimiter$kind"
		echo "$current_song_info" >> $tempSongFile
	else
		fSize=$(formatFileSize "$size")
		#echo "IGNORED << $c. Name: $name, Artist: $artist, Album: $album, Time: $time, Location: $location, rating: $rating, genre: $genre, year: $year, size: $fSize, kind: $kind"
		ignored=$((ignored + 1))
	fi
done

echo uploading...
$java -jar $musicOnCloud $tempSongFile $c $ignored
exit 0