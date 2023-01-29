#!/bin/bash -xv

export DISPLAY=:5

mode=$1
freq=$2
bw=$3
combitunerPath=$4
fifo=$5
symbolRate=$6
stdbuf -oL $combitunerPath -m $mode -f $freq -b $bw -s $symbolRate >> $fifo  &
#stdbuf -oL $combitunerPath -m $mode -f $freq -b $bw -s $symbolRate >> $fifo >/dev/null 2>/dev/null &
echo running in MODE $1 with FREQ $2 and Bandwidth $3
