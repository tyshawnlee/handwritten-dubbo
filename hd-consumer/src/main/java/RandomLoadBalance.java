import java.util.List;
import java.util.Random;

/**
 * @author litianxiang
 * @date 2020/3/17 18:24
 */
public class RandomLoadBalance {

	public String doSelect(List<String> providerList) {
		int size = providerList.size();
		Random random = new Random();
		return providerList.get(random.nextInt(size));
	}
}
