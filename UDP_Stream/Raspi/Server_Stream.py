"""
	UDP socket wrapped in a TCP socket to connect(TCP) and send frames(UDP)
    To a client
	
	v3 of the UDPServer series
"""

import socket, threading, pygame, math, sys, numpy
from SimpleCV import *

HOST, PORT = "192.168.1.202", 8675
SERVER_ADDR = (HOST, PORT)

TCP_BUFFER_SIZE = 1024

DEBUG = 0
HEADER_SIZE = 3
MAX_BUFFER_SIZE = 65507 - HEADER_SIZE	# 65507 bytes is the maxBuffer in UDP,
										# 3 bytes is the size of the header
encode_param=[int(cv2.IMWRITE_JPEG_QUALITY),90]

isConnected = False




#############################################




def tcpHandler(conn, addr):
	global isConnected
	
	isConnected	= True
	print "TCP connection established with:", addr[0]  + ":" + str(addr[1])

	# make a UDP server to send video
	udpSocket = socket.socket(socket.AF_INET, 	# Internet
						 socket.SOCK_DGRAM) # UDP
	udpSocket.bind(SERVER_ADDR)
	
	# get video quality
	while 1:		  
		data = conn.recv(TCP_BUFFER_SIZE) # buffer size
		if "quality=" in data:
			res = data.split("=")[1]
			break

	t = threading.Thread(target=tcpConnection, args=(conn,))
	t.start()
	

	# Get UDP addr connection of client
	while 1:
		data, clientAddr = udpSocket.recvfrom(TCP_BUFFER_SIZE)
		if data == "connect":
			break
			
	# Tell the client we are ready to send frames
	conn.sendall("connected")

	imageSender(udpSocket, clientAddr, res)
		
	print "Client has disconnected."
	
	return
	
def imageSender (udpSocket, clientAddr, res):
	global isConnected
	
	capture = cv2.VideoCapture(0)
	
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
	while isConnected:
		# Capture frame-by-frame
		ret, frame = capture.read()
		frameCount += 1

		# I then get the byte data of the encoded frame to send to the client
		encode_param=[int(cv2.IMWRITE_JPEG_QUALITY),90]
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
			
			udpSocket.sendto(data, clientAddr)
		
		# FPS meter
		clock.tick()
		print "FPS:", clock.get_fps(), "\tImage Size:", imgSize, "KB", "Chunks", chunks
		
	capture.release()
	
def tcpConnection(conn):
	global isConnected
	try:
		while 1:
			data = conn.recv(TCP_BUFFER_SIZE)
			if not data:
				raise Exception("connection closed")
	except Exception, e:
		isConnected = False
	return


if __name__ == "__main__":
	# Create the TCP server
	tcpSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	tcpSocket.bind(SERVER_ADDR)
	tcpSocket.listen(1)
	
	serverAddr = tcpSocket.getsockname()
	print "TCP Server running on:", serverAddr[0]  + ":" + str(serverAddr[1])
	
	while 1:
		try:
			conn, addr = tcpSocket.accept()
			tcpHandler(conn, addr)
		except KeyboardInterrupt, e:
			print "keyboard"
			sys.exit(0)
		except Exception, e:
			print "Exception:", str(e)
