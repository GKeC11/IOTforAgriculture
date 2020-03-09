import PyQt5.QtWidgets as qw
import PyQt5.QtGui as qg
import sys
import socket
import threading
import os
import re
import PyQt5.QtCore as qc
from PyQt5.QtGui import QRegion

class RecvThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self)

    def run(self): #服务端没有莫名其妙的输出就不写判断了
        while True:
            if isRecv == 0:
                message = str(client.recv(1024), 'utf-8')
                print(message)

class Gui(qw.QWidget):
    def __init__(self):
        super().__init__()
        self.Init()

    def Init(self):
        vbox = qw.QVBoxLayout(self)
        hbox = qw.QHBoxLayout()
        self.image_label = qw.QLabel()
        self.image_label.adjustSize()
        self.scroll_area = qw.QScrollArea()
        self.scroll_area.setWidget(self.image_label)
        self.image = qg.QPixmap()
        self.image_label.setPixmap(self.image)
        self.text = qw.QTextEdit()
        self.text.setFixedHeight(100)
        btn_pic = qw.QPushButton('Send_Pic')
        btn_text = qw.QPushButton("Send_Text")
        btn_pic.clicked.connect(self.onButtonClicked)
        btn_text.clicked.connect(self.onSendText)

        vbox.addWidget(self.scroll_area)
        vbox.addWidget(self.text)
        hbox.addWidget(btn_pic)
        hbox.addWidget(btn_text)
        vbox.addLayout(hbox)

        self.resize(800,600)
        self.show()

    def onButtonClicked(self):
        isRecv = 1
        client.setblocking(1)
        file_info = qw.QFileDialog.getOpenFileName(self,'Open File')
        fileName = file_info[0].split('/')[-1]
        type = re.findall(r'\.png$|\.jpg',fileName)
        if len(type) > 0:
            fileSize = os.path.getsize(file_info[0])
            client.send(fileName.encode())
            client.send(str(fileSize).encode())

            serverStatu = str(client.recv(1024),'utf-8')
            if serverStatu.find('get') == -1:
                progress = qw.QProgressDialog(None,None,0,fileSize,self)
                progress.setWindowModality(qc.Qt.WindowModal)
                for i in range(fileSize):
                    progress.setValue(i)
                else:
                    progress.setValue(fileSize)
                    qw.QMessageBox.information(self, "Info", "Got the response")

            with open(file_info[0],'rb') as file:
                for line in file:
                    client.sendall(line)

            #接收处理后的图片
            fileSize_r = int(str(client.recv(1024),'utf-8'))
            path = os.getcwd() + '\\image\\'
            if not os.path.exists(path):
                os.mkdir(path)
            fileName = path + fileName
            client.sendall("get".encode())
            with open(fileName,'wb') as file_r:
                recv_data = 0;
                while True:
                    data = client.recv(1024)
                    file_r.write(data)
                    recv_data += len(data)
                    if recv_data == fileSize_r:
                        isRecv = 0;
                        client.setblocking(0)
                        break;

            #显示处理后的图片
            self.image.load(fileName)
            self.image_label.setPixmap(self.image)
            self.image_label.adjustSize()

    def onSendText(self):
        message = self.text.toPlainText()
        client.sendall(message.encode())

if __name__ == "__main__":
    app = qw.QApplication(sys.argv)

    isRecv = 0;

    try:
        client = socket.socket()
        client.connect(('139.224.28.219',5000)) #139.224.28.219
        client.setblocking(0)
    except Exception:
        print("Can't connect")


    recvThread = RecvThread()
    recvThread.start()
    window = Gui()
    sys.exit(app.exec_())
