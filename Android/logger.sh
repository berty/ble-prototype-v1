#! /bin/bash

TAGS=(
	'advertise'
	'ble_manager'
	'connect_activity'
	'device'
	'device_manager'
	'gatt_client'
	'gatt_server'
	'main_activity'
	'scan'
)

PACKAGE='tech.berty.bletesting'

DEVICES=($(adb devices | grep '\tdevice' | cut -d'	' -f1))
CHOICE=""

if [ ${#DEVICES[@]} -eq 0 ]; then
	echo "No Android device found"
	exit
elif [ ${#DEVICES[@]} -eq 1 ]; then
	echo "One device found: ${DEVICES[0]}"
	CHOICE=${DEVICES[0]}
else
	echo "More than one device found:"

	while true; do
		echo
		COUNT=1
		for DEVICE in "${DEVICES[@]}"; do
			echo "	$COUNT. $DEVICE"
			COUNT=$((COUNT + 1))
		done

		echo
		echo -n "Your choice: "
		read INPUT

		REGEX='^[0-9]+$'
		if [[ !($INPUT =~ $REGEX) || $INPUT -eq 0 || $INPUT -gt ${#DEVICES[@]} ]] ; then
		   echo "Error: wrong choice!"
		else
			CHOICE=${DEVICES[((INPUT - 1))]}
			break
		fi
	done
fi

COMMAND="pidcat package $PACKAGE -s $CHOICE"

for TAG in "${TAGS[@]}"; do
	COMMAND="$COMMAND -t $TAG"
done

for ARG; do
	COMMAND="$COMMAND $ARG"
done

echo
echo $COMMAND

$COMMAND
