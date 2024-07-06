```
### set

```java
Set<X> set = catheter.set();
```
### array

```java
X[] array = catheter.array();
```

### dump

就如先前所说，```dump```会复制Catheter的元素到一个新的Catheter中

```java
Catheter<X> catheter = catheter.dump();
```

## 计数
这些操作可以统计Catheter中元素的个数

### 直接计数

```java
int count = catheter.count();
```

### 计数到AtomicInteger

```java
AtomicInteger i = new AtomicInteger();
catheter.count(i);
```

### 计数到Consumer

```java
Consumer<Integer> consumer = count -> {
    // The consumer codes.
};
catheter.count(consumer);

// 或者
catheter.count(count -> {
    // The consumer codes.
});
```

### 计数到Receptacle

```java
Receptacle<Integer> r = new Receptacle<>();
catheter.count(r);
```
