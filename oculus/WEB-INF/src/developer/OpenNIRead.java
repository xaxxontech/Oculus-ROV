package developer;

import org.OpenNI.*;

public class OpenNIRead implements IObserver<ErrorStateEventArgs>{
	
	public static final String SAMPLES_XML = "openNIconfig.xml";
	
	@Override
	public void update(IObservable<ErrorStateEventArgs> arg0,
			ErrorStateEventArgs arg1) {
		System.out.printf("Global error state has changed: %s", arg1.getCurrentError());
		System.exit(1);		
	}

	public static void main(String[] args) 
	{
		try 
		{
			OutArg<ScriptNode> scriptNodeArg = new OutArg<ScriptNode>();
			Context context = Context.createFromXmlFile(SAMPLES_XML, scriptNodeArg);
			
			OpenNIRead pThis = new OpenNIRead();
			
			context.getErrorStateChangedEvent().addObserver(pThis);
			
			DepthGenerator depth = (DepthGenerator)context.findExistingNode(NodeType.DEPTH);
			
			DepthMetaData depthMD = new DepthMetaData();
			
			while (true)
			{
				context.waitAnyUpdateAll();
				
				depth.getMetaData(depthMD);
				
				System.out.printf("Frame %d Middle point is: %d.\n", depthMD.getFrameID(), depthMD.getData().readPixel(depthMD.getXRes() / 2, depthMD.getYRes() / 2));
			}
		} 
		catch (Throwable e) 
		{
			e.printStackTrace();
		}
	}
	
}
