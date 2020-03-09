import socket
import threading
import re
from PIL import Image
import os

class SendThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

    def run(self):
        while True:
            if len(clients) > 0:
                client = clients[0]
                message = input()
                try:
                    client.sendall((message + '\n').encode())
                except:
                    del clients[0]


class RecvThread(threading.Thread):
    def __init__(self,connect):
        threading.Thread.__init__(self)
        self.conn = connect

    def run(self):
        while True:
            message = str(self.conn.recv(1024),'utf-8')
            type = re.findall(r'\.png$|\.jpg',message)

            if len(type) > 0:
                fileSize = int(str(self.conn.recv(1024), 'utf-8'))
                self.conn.sendall(('get' + '\n').encode())
                with open(message,'wb') as file:
                    recv_size = 0
                    while True:
                        data = self.conn.recv(1024)
                        file.write(data)
                        recv_size += len(data)
                        print("Recv: "+ str(recv_size))
                        if recv_size == fileSize:
                            break
                #处理图片并返回
                im = Image.open(message)
                im = im.convert('L')
                im.save(message)
                fileSize_r = os.path.getsize(message)
                print("return filesize is :" + str(fileSize_r))
                self.conn.sendall((str(fileSize_r) + '\n').encode())

                flag = ""
                while (flag.find("get")) == -1:
                    flag = str(self.conn.recv(1024), 'utf-8')

                print(flag)
                with open(message,'rb') as file_return:
                    for line in file_return:
                        self.conn.sendall(line)

            if len(message) > 0:
                print(message)


if __name__ == "__main__":
    Host = '172.17.48.5' #172.17.48.5
    Port = 5000

    socket = socket.socket()
    clients = []
    socket.bind((Host,Port))
    sendThread = SendThread()
    sendThread.start()
    socket.listen(3)
    while True:
        conn,addr = socket.accept()
        t = RecvThread(conn)
        t.start()
        clients.append(conn)