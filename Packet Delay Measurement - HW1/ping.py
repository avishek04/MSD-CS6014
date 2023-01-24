import subprocess

#This function will be executed after every x minutes

def Ping():

        f1 = open("ping.txt","w")
        ip="95.142.107.181"
        f1.write(ip+": ")

        ping = subprocess.Popen(['ping', '-c', '200', ip],stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    
        while (True):
                hop = ping.stdout.readline()

                if not hop: break

                print('-->',hop)
                line = str(hop, 'UTF-8')
                f1.write(line)
                
Ping() 
