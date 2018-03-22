# Model Compression

- Bucilua, C., Caruana, R. and Niculescu-Mizil, A.: Model Compression, Proc. ACM SIGKDD, pp. 535–541 (2006).
- https://dl.acm.org/citation.cfm?id=1150464


## munge

### データの用意

- Standardization

101行目から
MUNGEでは各特徴量が連続であるか否かを入力として与える必要がある．

以下は全ての特徴量が連続の場合の例

データに応じて`l`を変更する．

```java
// represent instances attributes for munge
// 0 : continuous
// 1 : other
int[] l = new int[m_numAttr];
Arrays.fill(l, 0);
```


### 実行

```sh
cd MUNGE
javac -classpath Extentions/py4j.jar:Extentions/weka.jar MUNGE.java
python munge.py
```

パラメータの設定

108~110行目をいじる

```java
double p = 0.5;
double s = 10;
int k = 1000000;
```

- Input:  `data/data.npy`
- Output: `data/data.arff`



## dependencies

- weka: https://www.cs.waikato.ac.nz/ml/weka/
- py4j: https://www.py4j.org/
