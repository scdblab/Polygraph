#!/bin/bash


nums=$(echo $2 | tr "," "\n")

for i in $nums
do

let " numV_half = $i / 2 "
echo "Running "$numV_half" Validators on localhost and "$numV_half" Validators on another"

./validScript_Multi.sh $1 $i $3 $4 0



done






