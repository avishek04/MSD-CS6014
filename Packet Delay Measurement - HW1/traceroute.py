import subprocess

#This function will be executed after every x minutes

def TraceRoute():

        f1 = open("output1.txt","w")
        f2 = open("plot1.txt","w")
        hostname="utah.edu"
        f1.write(hostname+": ")
        f2.write(hostname+": ")
#        print(hostname)

#        subprocess.call(['traceroute',hostname])

        traceroute = subprocess.Popen(['traceroute', '-w', '100', hostname],stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        
#        for line in iter(traceroute.stdout.readline,""):
#            print(line)
##            f.write(str(line, 'UTF-8'))
#
#        f.close()
        while (True):
                hop = traceroute.stdout.readline()

                if not hop: break

                print('-->',hop)
                line = str(hop, 'UTF-8')
                f1.write(line)
                
                strList = line.split()
                delayTime = 0
                delayCount = 0
                ip = ""

                for data in strList:
                        if data[0] == '(' and data[len(data) - 1] == ')':
                                ip = data

                        if len(data) == 5:
                                delayTime += float(data)
                                delayCount += 1

                if ip != "" and delayCount > 0:
                        f2.write(ip + " " + str(delayTime/delayCount) + " \n")
                else:
                        f2.write("* 0 \n")

                # if (len(strList) == 9):
                #         newLine = strList[1] + " " + (float(strList[4]) + float(strList[6]) + float(strList[8]))/3
                #         f2.write(newLine)

                # if (strList[2] == '*' and strList[3] == '*' and strList[4] == '*'):
                #         newLine = "* 0ms"


#        traceroute = subprocess.Popen(["traceroute", '-w', '100',hostname],stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
#
#        for line in iter(traceroute.stdout.readline,""):
#            print(line)
            
#        threading.Timer(60*50, TraceRoute).start() #Ensures periodic execution of TraceRoute( ) x=60*50 seconds

TraceRoute() 
