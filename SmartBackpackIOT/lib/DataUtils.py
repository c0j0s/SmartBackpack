def getComfortLevel(level):
    levels = {
    '0' : 'nil',
    '1' : 'dry',
    '2' : 'ok',
    '3' : 'ok ~',
    '4' : 'can',
    '5' : 'uncomfortable',
    '6' : 'very uncomfortable',
    '7' : 'very uncomfortable ~',
    }

    return levels[level]
def getTempByRange(temp):
    y = 46 - int(temp)
    return y

def getHumByRange(hum):
    hum = roundHum(int(hum))

    x = round(hum/5) - 1

    return x

def getLeveByHum(hum):
    if hum > 80:
        hum = 2
    elif hum > 60 and hum <= 80: 
        hum = 1
    elif hum > 40 and hum <= 60: 
        hum = 0
    elif hum > 20 and hum <= 40: 
        hum = 1
    elif hum <= 20: 
        hum = 2
    return hum


def roundHum(x, base=5):
    return int(base * round(float(x)/base))