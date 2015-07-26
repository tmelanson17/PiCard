import io
import time
import picamera
import SocketServer

class Camera():
    def capture():
        # Create an in-memory stream
        my_stream = io.BytesIO()
        with picamera.PiCamera() as camera:
            camera.start_preview()
            # Camera warm-up time
            time.sleep(2)
            camera.capture(my_stream, 'jpeg')


class MyTCPHandler(SocketServer.BaseRequestHandler):
    """
        The RequestHandler class for our server.
        
        It is instantiated once per connection to the server, and must
        override the handle() method to implement communication to the
        client.
        """
    
    
    
    def handle(self):
        # self.request is the TCP socket connected to the client
        self.data = self.request.recv(1024).strip()
        print "{} wrote:".format(self.client_address[0])
        print self.data
            my_stream.write(self.data)
        # just send back the same data, but upper-cased
        self.request.sendall()

if __name__ == "__main__":
    HOST, PORT = "localhost", 9999
    
    # Create the server, binding to localhost on port 9999
    server = SocketServer.TCPServer((HOST, PORT), MyTCPHandler)
    
    # Activate the server; this will keep running until you
    # interrupt the program with Ctrl-C
    server.serve_forever()