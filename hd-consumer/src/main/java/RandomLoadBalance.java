import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡算法
 * @author litianxiang
 * @date 2020/3/17 18:24
 */
public class RandomLoadBalance {

	/**
	 * 随机一个provider
	 * @param providerList provider列表
	 * @return provider
	 */
	public String doSelect(List<String> providerList) {
		int size = providerList.size();
		Random random = new Random();
		return providerList.get(random.nextInt(size));
	}
}
