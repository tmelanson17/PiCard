"""
	Not used!!!
	
	Send any size livestream to a connect client
"""
from SimpleCV import *
import numpy as np
import socket, pygame, math, sys

DEBUG = 0
HEADER_SIZE = 3
MAX_BUFFER_SIZE = 65507 - HEADER_SIZE	# 65507 bytes is the maxBuffer in UDP,
										# 3 bytes is the size of the header
encode_param=[int(cv2.IMWRITE_JPEG_QUALITY),90]

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
res = "144p"
if not DEBUG:
	while 1:		  
		data, addr = sock.recvfrom(MAX_BUFFER_SIZE) # buffer size
		if "start" in data:
			res = data.split(" ")[1]
			break


# Set the capture to the Raspicam
#2560x1920
if (res == "240p"):
	x = 240
	y = 320
elif (res == "360p"):
	x = 360
	y = 480
elif (res == "480p"):
	x = 480
	y = 640
elif (res == "720p"):
	x = 1280
	y = 720
else:
	x = 192
	y = 144
	
print 'Connection address:', addr, " @", res, ":"

capture = cv2.VideoCapture(0)
capture.set(cv.CV_CAP_PROP_FRAME_WIDTH, x)
capture.set(cv.CV_CAP_PROP_FRAME_HEIGHT, y)

#clock to calculate FPS
clock = pygame.time.Clock()

"""
	Here is the main loop of the program.
	We repeatly get frames from the capture and
	send that image to the client using the UDP socket
"""
frameCount = 0
while 1:
	# Capture frame-by-frame
	ret, frame = capture.read()
	frameCount += 1

	# I then get the byte data of the encoded frame to send to the client
	result, imgencode = cv2.imencode('.jpg', frame, encode_param)
	data = np.array(imgencode)
	imgString = data.tostring()
	
	imgSize = len(imgString)
	
	# convert a number to float first.. http://stackoverflow.com/a/7950627
	chunks = int(math.ceil(imgSize/float(MAX_BUFFER_SIZE)))
	for i in range(0, chunks):
		chunkSize = MAX_BUFFER_SIZE if (i != chunks-1) else (imgSize - (i * MAX_BUFFER_SIZE))

		index = i * MAX_BUFFER_SIZE
		# send metadata about the frame
		header = [
					chr(frameCount & 0xFF),		# current frame
					chr(i+1 & 0xFF),				# current chunk
					chr(chunks & 0xFF)			# total chunks
				 ]

		packet = "".join(header)
		data = bytearray()
		data.extend(packet)
		data.extend(imgString[index: index+chunkSize])
		
		sock.sendto(data, addr)
	
	# FPS meter
	clock.tick()
	print "FPS:", clock.get_fps(), "\tImage Size:", imgSize, "KB", "Chunks", chunks
	

# When everything done, release the capture
capture.release()
