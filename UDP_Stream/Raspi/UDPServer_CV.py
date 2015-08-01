"""
	Not used!!!
	
	Simple UDP Server to send 1 frame under 65507 bytes to a client
"""

from SimpleCV import *
import numpy as np
import socket, pygame

UDP_IP = "192.168.1.202"	# local IP of the Raspi,
							# you will need to change it if you are running this code
							
UDP_PORT = 8675				# a random port

sock = socket.socket(socket.AF_INET,	# ipv4 ip address
			  socket.SOCK_DGRAM) 		# UDP socket

sock.bind((UDP_IP, UDP_PORT)) # bind the server socket to the ip and port 

print "UDP Bind IP:", UDP_IP
print "UDP Bind port:", UDP_PORT

addr = (UDP_IP, UDP_PORT)

"""
	Here we wait to establish a connection to another device.
	The python script has to run before the Android device starts the app.
"""
while 1:		  
	data, addr = sock.recvfrom(65507) # buffer size
	if data == "start":
		print "Starting stream to: " + str(addr)
		break


# Set the capture to the Raspicam
cap = cv2.VideoCapture(0)

#clock to calculate FPS
clock = pygame.time.Clock()

"""
	Here is the main loop of the program.
	We repeatly get frames from the capture and
	send that image to the client using the UDP socket
"""
while 1:
	# Capture frame-by-frame
	ret, frame = cap.read()

	
	# I chose to rotate the image because it is hard for me
	# to put the camera at an upright angle
	tup = frame.shape
	M = cv2.getRotationMatrix2D((tup[0]/2, tup[1]/2), 90, 1)
	frame = cv2.warpAffine(frame, M, (tup[0], tup[1]))

	# I then get the byte data of the encoded frame to send to the client
	imgString = cv2.imencode('.png', frame)[1].tostring()
	sock.sendto(imgString, addr)
	
	# FPS meter
	clock.tick()
	print str(clock.get_fps())
	

# When everything done, release the capture
cap.release()
