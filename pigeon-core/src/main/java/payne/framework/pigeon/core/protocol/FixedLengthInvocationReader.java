package payne.framework.pigeon.core.protocol;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import payne.framework.pigeon.core.Header;
import payne.framework.pigeon.core.Invocation;
import payne.framework.pigeon.core.factory.bean.BeanFactory;
import payne.framework.pigeon.core.factory.stream.StreamFactory;
import payne.framework.pigeon.core.formatting.FormatInvocationInputStream;
import payne.framework.pigeon.core.formatting.InvocationFormatter;
import payne.framework.pigeon.core.formatting.Structure;
import payne.framework.pigeon.core.formatting.URLInvocationFormatter;
import payne.framework.pigeon.core.processing.InvocationFormatProcedure;
import payne.framework.pigeon.core.processing.Step;
import payne.framework.pigeon.core.toolkit.FixedLengthInputStream;
import payne.framework.pigeon.core.toolkit.IOToolkit;
import payne.framework.pigeon.core.toolkit.ReadableInputStream;

/**
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Company: 广州市俏狐信息科技有限公司
 * </p>
 * 
 * @author yangchangpei 646742615@qq.com
 *
 * @date 2015年10月18日 下午4:16:15
 *
 * @version 1.0.0
 */
public class FixedLengthInvocationReader implements HTTPInvocationReader {
	private final Channel channel;
	private final Header clientHeader;

	public FixedLengthInvocationReader(Channel channel, Header clientHeader) {
		super();
		this.channel = channel;
		this.clientHeader = clientHeader;
	}

	public Invocation read(Method method, BeanFactory beanFactory, StreamFactory streamFactory, List<Step> steps) throws Exception {
		InputStream wrap = null;
		try {
			wrap = new FixedLengthInputStream(new ReadableInputStream(channel), clientHeader.getContentLength());
			InvocationFormatter formatter = clientHeader.getContentType() != null ? beanFactory.get(clientHeader.getContentType(), InvocationFormatter.class) : new URLInvocationFormatter();
			InvocationFormatProcedure procedure = new InvocationFormatProcedure(formatter, Structure.forArray(method.getGenericParameterTypes(), method.getParameterAnnotations()));
			Step step = new Step(null, null, procedure);
			steps.add(0, step);

			for (int i = 0; i < steps.size(); i++) {
				Step s = steps.get(i);
				s.initialize(Channel.SIDE_SERVER, beanFactory, streamFactory, channel, clientHeader);
			}

			for (int i = steps.size() - 1; i >= 0; i--) {
				Step s = steps.get(i);
				wrap = s.wrap(Channel.SIDE_SERVER, beanFactory, streamFactory, channel, clientHeader, wrap);
			}

			FormatInvocationInputStream fiis = (FormatInvocationInputStream) wrap;
			Invocation invocation = new Invocation();
			Object data = fiis.deserialize();
			invocation.setArguments(data == null ? new Object[method.getParameterTypes().length] : (Object[]) data);
			invocation.setClientHeader(clientHeader);
			return invocation;
		} finally {
			IOToolkit.close(wrap);
		}
	}

}
