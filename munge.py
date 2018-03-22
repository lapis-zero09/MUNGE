import subprocess
import time
import numpy as np
import sys
import array
from py4j.java_gateway import JavaGateway


def calc_munge_on_java(data, gateway_inst):
    header = array.array('i', list(data.shape))
    body = array.array('d', data.flatten().tolist())
    if sys.byteorder != 'big':
        header.byteswap()
        body.byteswap()
    buf = bytearray(header.tostring() + body.tostring())
    java_matrix = gateway_inst.munge(buf)
    return java_matrix


if __name__ == '__main__':
    args = (['java', '-Xmx4096g', '-cp',
             'Extentions/weka.jar:Extentions/py4j.jar:./',
             'MUNGE'])
    p = subprocess.Popen(args)
    # サーバー起動前に処理が下へ行くのを防ぐ
    time.sleep(3)
    # JVMへ接続
    gateway = JavaGateway(start_callback_server=True)
    gateway_inst = gateway.entry_point

    try:
        x = np.load('./data/X_train.npy')
    except:
        x = np.load('./data/X_train.npz')['d']

    try:
        munge = calc_munge_on_java(x, gateway_inst)
    except:
        gateway.shutdown()
        raise Exception('[!] Error from Java...')

    # print(munge)

    gateway.shutdown()
    print('    [*] Success for shutdown Java...')
    print()
