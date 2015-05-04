#!/usr/bin/env python

# Import python numerical processing libraries
from numpy import *
from scipy import *
from pylab import *
import time

# Tries to match the pattern with elements in the signals list starting at signalStartIndex
def matchList(signals,  signalsStartIndex,  pattern):
    patternLength = len(pattern)
    signalsLength = len(signals)
    
    # Make sure there are enough elements left in the list to compare with
    if signalsLength - signalsStartIndex  - 1 < patternLength:
        return False
    
    # Iterate through each element to check if they match
    for i in range (0, patternLength):
        if (signals[signalsStartIndex+i] != pattern[i]):
            return False
    
    # If we get to the end it is a match
    return True
    
# END matchList


# Processes the data array from the index
def processSamples(data,  startIndex):

    dataLength = len(data)
#    print "Loaded %d samples"%(dataLength)

    # Define some constants
    ZERO = 0;
    ONE = 1;
    GAP = -1

    # Set some thresholds
    magnitudeThreshold = 0.1
    shortSignalWidth = [200, 600]
    longSignalWidth = [1000, 1600]
    gapSignalWidth = 16000

    signals = list()
    i=startIndex
    stepSize = 10 # increment i by stepSize each iteration
    while i < dataLength:

        # Check each element looking for a value that crosses ABOVE the threshold
        counter = 0
        while   i < dataLength and data[i] >= magnitudeThreshold:
            # Update these variables for each iteration
            counter +=  1
            i += stepSize

        # If we found samples that cross above the threshold then measure the width
        if counter >= shortSignalWidth[0]/stepSize and counter <= shortSignalWidth[1]/stepSize:
            signals.append(ZERO)
        elif counter >= longSignalWidth[0]/stepSize and counter <= longSignalWidth[1]/stepSize:
            signals.append(ONE)

        # Check each element looking for a value that crosses BELOW the threshold
        counter = 0
        while   i < dataLength and data[i] < magnitudeThreshold:
            # Update these variables for each iteration
            counter +=  1
            i += stepSize

        # If we found samples that cross below the threshold then measure the width
        if counter >= gapSignalWidth/stepSize:
            signals.append(GAP)

        # Increment i
        i += stepSize

#    print "Finished reading samples into ",  len(signals),  " signals"
    
    prefix = [0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0,1, 0, 1, 0, 1]
    prefixLength = len(prefix)

    codeA = [0, 0, 0, 0, 0, 0, 1, 1, 0]
    codeB = [0, 0, 0, 0, 1, 1, 0, 0, 0]
    codeC = [0, 0, 1, 1, 0, 0, 0, 0, 0]
    codeD = [1, 1, 0, 0, 0, 0, 0, 0, 0]
    codeLength = len(codeA)

    i=0
    signalsLength = len(signals)

    # Don't start trying to match until we've first encountered a gap.
    # If we don't do this, then holding down a button will register multiple presses when it shouldn't
    while i < signalsLength and signals[i] != GAP:
        i += 1

    while i <  signalsLength:

        matchFound = False
        
        # Now start searching for matching patterns
        if (matchList(signals, i, prefix)):
            # Looks for the prefix starting from index in the signals array
            i+= prefixLength
            
            # Try to match the code. The first test in the conditional is a pre-emptive check to avoid having to do a full match
            if (matchList(signals, i, codeA)):
                print "A"
                matchFound = True
                i += codeLength
            elif  (matchList(signals, i, codeB)):
                print "B"
                matchFound = True
                i += codeLength
            elif  (matchList(signals, i, codeC)):
                print "C"
                matchFound = True
                i += codeLength
            elif  (matchList(signals, i, codeD)):
                print "D"
                matchFound = True
                i += codeLength
            
        # Before we can search for another button press match, there needs to be a gap
        # If there is no gap it means someone was holding down a button
        while matchFound:
            if  i >= signalsLength or signals[i] == GAP:
                # We can quit the loop
                matchFound = False

            i += 1
            
        # Increment i for the loop
        i += 1


# END processSamples



# -----------------
# Main Code
# -----------------

print
print "Press ctrl+C to quit"
print

# Sampling rate is 1M per second
sampleRate = 1e6

# Keeps track of where we are in the data array
index = 0
while True:
    
    # Load in data
    data = memmap("output.float32", dtype=float32)
    dataLength = len(data)

	# If we have more than a quarter of a second of new data then process it
    if (dataLength - index) > sampleRate/2:
        processSamples(data,  index,)
        index = dataLength - 1
    else:
        time.sleep(0.1)









