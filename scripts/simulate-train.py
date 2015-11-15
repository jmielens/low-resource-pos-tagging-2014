import sys, re, os, glob, random, time, operator
from optparse import OptionParser
from numpy import *
from subprocess import call,Popen,PIPE

parser = OptionParser()
parser.add_option("-f", "--file", dest="replacefile",
              help="Replacement File")
parser.add_option("-o", "--output", dest="outputfile", default="out.conll",
              help="Output File")
parser.add_option("-t", "--train", dest="trainfile",
              help="File to extract training material from")
parser.add_option("-m", "--model", dest="modelfile", default="temp.ser",
              help="Location to store model")
parser.add_option("-n", "--hours", dest="hours", default=2, type="int",
              help="Number of simulated annotation hours")
parser.add_option("-y", "--types-per-hour", dest="typesperhour", default=100, type="int",
              help="Number of type annotations done per hour")
parser.add_option("-x", "--skip-training", dest="skiptraining", action="store_true", default=False,
              help="Location to store model")
(options, args) = parser.parse_args()


if not options.skiptraining:
	tokens = []
	typesToTake = options.hours*options.typesperhour

	# Read Training CoNLL File and build Form|Tag Pairs
	rawSentences = open(options.trainfile).read().split("\n\n")
	rawSentences = [s for s in rawSentences if len(s) > 0 and s[0] == "1"]
	for sentence in rawSentences:
		for line in sentence.split("\n"):
			if len(line.split("\t")) > 5:
				tokens.append((line.split("\t")[1],line.split("\t")[3]))

	# Write Raw Training Sentences
	rawTraining = open("temp.raw",'w')
	rawTrainingSentences = [" ".join([line.split("\t")[1] for line in sentence.split("\n") if len(line.split("\t")) > 5]) for sentence in rawSentences]
	for line in rawTrainingSentences:
		rawTraining.write(line+"\n")
	rawTraining.close()

	# Write Type Training File
	tokenCounts = {token:tokens.count(token) for token in tokens}
	sortedTokenCounts = sorted(tokenCounts.items(), key=operator.itemgetter(1))
	sortedTokenCounts.reverse()

	typeTraining = open("temp.type",'w')
	for tokenType in sortedTokenCounts[0:typesToTake]:
		typeTraining.write(tokenType[0][0]+"|"+tokenType[0][1]+"\n")
	typeTraining.close()

	# Train Tagger
	call(["./run", "--rawFile", "temp.raw", "--typesupFile", "temp.type", "--modelFile", options.modelfile])


# Convert Target File to Raw Sentences
rawSentences = open(options.replacefile).read().strip().split("\n\n")
rawSentences = [s for s in rawSentences if len(s) > 0 and s[0] == "1"]
replaceSentences = [" ".join([line.split("\t")[1] for line in sentence.split("\n") if len(line.split("\t")) > 5]) for sentence in rawSentences]
replaceRaw = open("temp.replace",'w')
for sentence in replaceSentences:
	replaceRaw.write(sentence+"\n")
replaceRaw.close()

# Tag Target Sentences
call(["./run", "--modelFile", options.modelfile, "--inputFile", "temp.replace", "--outputFile", "temp.tagged"])

# Read New Target Taggings
targets = open("temp.tagged").read().strip().split("\n")

# Write Output
out = open(options.outputfile,'w')
for (sentence,target) in zip(rawSentences,targets):
	for (line,token) in zip(sentence.split("\n"), target.split(" ")):
		tokens = line.split("\t")
		tokens[3] = token.split("|")[1]
		out.write("\t".join(tokens)+"\n")
	out.write("\n")
out.close()

Popen('rm temp.*', shell=True,
                   stdout=PIPE,
                   stderr=PIPE)