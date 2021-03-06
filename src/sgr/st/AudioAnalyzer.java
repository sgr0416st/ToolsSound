package sgr.st;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.sound.sampled.AudioFormat;

public class AudioAnalyzer {

	private Queue<Integer> average_que;
	private AudioFormat format;
	private ByteOrder order;

	/**
	 * 扱うオーディオのフォーマット、監視するセグメントの数を指定して
	 * 音声解析機を構築します。
	 *
	 * @param format  扱うオーディオのフォーマット.
	 * @param moniteringSegmentNum 直近のデータと比較をする際に保存しておくセグメントの数。
	 */
	public AudioAnalyzer(AudioFormat format, int moniteringSegmentNum) {
		this.format = format;
		average_que = new ArrayBlockingQueue<Integer>(moniteringSegmentNum);
		if(format.isBigEndian()) {
			order = ByteOrder.BIG_ENDIAN;
		}else {
			order = ByteOrder.LITTLE_ENDIAN;
		}
	}

	/**
	 * サウンドデータが閾値以上の音圧レベルかどうかを調べます。
	 *
	 * @param segment サウンドデータの一部
	 * @param threashold 閾値
	 * @return 与えられたデータが閾値以上ならtrue, それ以外ならfalse
	 */
	public static boolean isOver(byte[] segment, int threashold) {
		for(byte d: segment) {
			if(d < threashold) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 急激な音圧の変化を検出する関数
	 *
	 * @param segment 現在の音声シーケンス
	 * @param length シーケンスに含まれる実データの長さ
	 * @return 直前の音圧と比較した変化量
	 */
	public int detectDiff(byte[] segment, int length) {
		int average = 0, diff = 0;
		ByteBuffer bb = ByteBuffer.wrap(segment);
		bb.order(order);

		if((bb.remaining() % format.getFrameSize()) != 0) {
			return -1;
		}else {
			// 現在のsegment の平均値計算
			int rest = segment.length - length;
			while(bb.remaining() > rest) {
				try {
					if(format.getFrameSize() == 2) {
						average += Math.abs(bb.getShort());
					}else {
						average += Math.abs(bb.get());
					}
				}catch (BufferOverflowException  e) {}
			}
			average /= (length/format.getFrameSize());

			// 保存してある直前の数セグメントと現在のセグメントの音圧の平均値を比較
			if(!average_que.offer(average)) {
				int allav = 0;
				for(Integer av: average_que) {
					allav += av.intValue();
				}
				allav /= average_que.size();
				diff = average - allav;

				// 新しいセグメントの保存と古いセグメントの破棄
				average_que.poll();
				average_que.offer(average);
			}
			return diff;
		}
	}
	/**
	 * 急激な音圧の変化を検出する関数
	 *
	 * @param segment 現在の音声シーケンス
	 * @return 直前の音圧と比較した変化量
	 */
	public int detectDiff(byte[] segment) {
		return detectDiff(segment, segment.length);
	}

}
