import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.transform.AttachVolumeRequestMarshaller;

public class EbsVolume  {
	
	
	
	static AmazonEC2      ec2;
	
	public static void main(String[] args) throws Exception {
		
		AWSCredentials credentials = null;
        try {
        	credentials = new PropertiesCredentials(
   			EbsVolume.class.getResourceAsStream("AwsCredentials.properties"));
        } catch (IOException e1){
       	 System.out.println(" Wrong Credentials entered into AwsCredentials.properties.");
		        System.out.println(e1.getMessage());
		        System.exit(-1);
        }
        
        /*********************************************
         * 
         *  #1 Create Amazon Client object
         *  
         *********************************************/
   	 System.out.println("#1 Create Amazon Client object");
        ec2 = new AmazonEC2Client(credentials);

        
      
       try {
       	
       	/*********************************************
       	 * 
            *  #2 Describe Availability Zones.
            *  
            *********************************************/
       	System.out.println("#2 Describe Availability Zones.");
           DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
           System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                   " Availability Zones.");

           /*********************************************
            * 
            *  #3 Describe Available Images
            *  
            *********************************************/
           System.out.println("#3 Describe Available Images");
           DescribeImagesResult dir = ec2.describeImages();
           List<Image> images = dir.getImages();
           System.out.println("You have " + images.size() + " Amazon images");
           
           
           /*********************************************
            *                 
            *  #4 Describe Key Pair
            *                 
            *********************************************/
           System.out.println("#4 Describe Key Pair");
           DescribeKeyPairsResult dkr = ec2.describeKeyPairs();
           System.out.println(dkr.toString());
           
           /*********************************************
            * 
            *  #5 Describe Current Instances
            *  
            *********************************************/
           System.out.println("#5 Describe Current Instances");
           DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
           List<Reservation> reservations = describeInstancesRequest.getReservations();
           Set<Instance> instances = new HashSet<Instance>();
           // add all instances to a Set.
           for (Reservation reservation : reservations) {
           	instances.addAll(reservation.getInstances());
           }
           
           System.out.println("You have " + instances.size() + " Amazon EC2 instance(s).");
           for (Instance ins : instances){
           	
           	// instance id
           	String instanceId = ins.getInstanceId();
           	
           	// instance state
           	InstanceState is = ins.getState();
           	System.out.println(instanceId+" "+is.getName());
           }
           
           /*********************************************
            *  #6 Security Group and Key creation
            *********************************************/
           System.out.println("#6 Security Group and Key creation.");
       	String grpName = "Group";
       	String keyName = "Key";
       	boolean keyflag=true;
       	boolean groupflag=true;
       	
           List<KeyPairInfo> keypair = ec2.describeKeyPairs().getKeyPairs();
           for (KeyPairInfo kp : keypair){
           	if (kp.getKeyName().equals(keyName))
           	{
           		System.out.println("\tThe Key is already present.");
           		keyflag = false;
           	}
           }
           
           if (keyflag){
           	System.out.println("\tCreate key value.");
               CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
               
               createKeyPairRequest.withKeyName(keyName);
               CreateKeyPairResult createKeyPairResult = ec2.createKeyPair(createKeyPairRequest);

               KeyPair keyPair = new KeyPair();
               keyPair = createKeyPairResult.getKeyPair();
               String privateKey = keyPair.getKeyMaterial();
               
               //System.out.println("privateKey: " + privateKey);
               
               try {
       			File file = new File(keyName+".pem");
       			 
       			// if file doesn't exists, then create it
       			if (!file.exists()) {
       				file.createNewFile();
       			}
        
       			FileWriter fw = new FileWriter(file.getAbsoluteFile());
       			BufferedWriter bw = new BufferedWriter(fw);
       			bw.write(privateKey);
       			bw.close();
       			
                 } catch ( IOException e ) {
                    e.printStackTrace();
                 }
           }
           	
           List<SecurityGroup> Groupname = ec2.describeSecurityGroups().getSecurityGroups();
           for (SecurityGroup gn : Groupname){
           	if (gn.getGroupName().equals(grpName)){
           		System.out.println("\tThe Group is already present.");
           		groupflag = false;
           	}	
           }
           
       	if (groupflag){
       		
               try{
               	
               	System.out.println("\tCreate security Group.");
                   CreateSecurityGroupRequest createGroupRequest = new CreateSecurityGroupRequest();
                   createGroupRequest.withGroupName(grpName).withDescription("Created through AWS java SDK.");
                   
                   CreateSecurityGroupResult createGroupResult = ec2.createSecurityGroup(createGroupRequest);
                   
                   //Create Security Group Permission
                   Collection<IpPermission> ips = new ArrayList<IpPermission>();

                   // Permission for SSH only to your ip
                   IpPermission ipssh = new IpPermission();
                   ipssh.withIpRanges("0.0.0.0/0")
                   						 .withIpProtocol("tcp")
                   						 .withFromPort(22)
                   						 .withToPort(22);
                   ips.add(ipssh);
                   
                   // Permission for HTTP only to your ip
                   IpPermission iphttp = new IpPermission();
                   iphttp.withIpRanges("0.0.0.0/0")
                   						 .withIpProtocol("tcp")
                   						 .withFromPort(80)
                   						 .withToPort(80);
                   ips.add(iphttp);
   	 
                   			
                   AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
                   			 new AuthorizeSecurityGroupIngressRequest();
                   
                   authorizeSecurityGroupIngressRequest.withGroupName(grpName)
                   			 						.withIpPermissions(ips);
                   
                   ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);

                   }
                   catch(AmazonServiceException ase){
                   	// Likely this means that the group is already created, so ignore.
                         	System.out.println("\tError:" + ase.getMessage());
                   }
       	}
           
           /*********************************************
            * 
            *  #7 Create an Instance
            *  
            *********************************************/
           System.out.println("#7 Create an Instance");
           String imageId = "ami-76f0061f"; //Basic 32-bit Amazon Linux AMI
           int minInstanceCount = 1; // create 1 instance
           int maxInstanceCount = 1;
           String insType = "t1.micro";
           String publicIP = null;
           String publicDNS = null;
           RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
           rir.withInstanceType(insType).withKeyName(keyName).withSecurityGroups(grpName);
           RunInstancesResult result = ec2.runInstances(rir);
           
           
           //get instanceId from the result
           List<Instance> resultInstance = result.getReservation().getInstances();
           String createdInstanceId = null;
           for (Instance ins : resultInstance){
           	createdInstanceId = ins.getInstanceId();
           	System.out.println("New instance has been created: "+ins.getInstanceId());
           
           
           DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest().withInstanceIds(createdInstanceId);
       	DescribeInstanceStatusResult describeInstanceResult = ec2.describeInstanceStatus(describeInstanceRequest);
       	List<InstanceStatus> state1 = describeInstanceResult.getInstanceStatuses();
       	while (state1.size() < 1) { 
       	    // Do nothing, just wait, have thread sleep if needed
       	    describeInstanceResult = ec2.describeInstanceStatus(describeInstanceRequest);
       	    state1 = describeInstanceResult.getInstanceStatuses();           	    
       	}
       	
           describeInstancesRequest = ec2.describeInstances();
           reservations = describeInstancesRequest.getReservations();
           
           // add all instances to a Set.
           for (Reservation reservation : reservations) {
           	instances.addAll(reservation.getInstances());
           }
           
           
           for (Instance ins1 : instances){
           	
           	// instance id
           	String instanceId = ins1.getInstanceId();
           	if (instanceId.equals(createdInstanceId)){
               	publicIP = ins1.getPublicIpAddress();
               	publicDNS = ins1.getPublicDnsName();
           	}
           }

       	System.out.println("\tPublic IP: "+ publicIP + "\n\tPublic DNS: "+ publicDNS );	
       }
           
           
           
           /*********************************************
            * 
            *  #8 Attach EBS Volume to the instance.
            *  
            *********************************************/
           System.out.println("#8 Attaching EBS volume to EC2");
        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest()
        .withAvailabilityZone("us-east-1b")
        .withSize(2); // The size of the volume, in gigabytes.
        Thread.sleep(2000);
    	
    	CreateVolumeResult createVolumeResult = ec2.createVolume(createVolumeRequest);
    	
    	Thread.sleep(2000);
    	Volume v1 = createVolumeResult.getVolume();
        String volumeID = v1.getVolumeId();
        Thread.sleep(2000);
       
        
    	AttachVolumeRequest attachRequest = new AttachVolumeRequest()
        .withInstanceId(createdInstanceId)
        .withVolumeId(volumeID)
        .withDevice("/dev/sdh");
    	
    	
    	Thread.sleep(3000);
    	AttachVolumeResult attachResult = ec2.attachVolume(attachRequest); 
    	
    	
    	System.out.println("The EBS volume has been attached and volume ID is:" +volumeID );
    	
    	/*********************************************
         * 
         *  #9 Create a 'tag' for the new instance.
         *  
         *********************************************/
        System.out.println("#9 Create a 'tag' for the new instance.");
        List<String> resources = new LinkedList<String>();
        List<Tag> tags = new LinkedList<Tag>();
        Tag nameTag = new Tag("Name", "MyFirstInstance");
        
        resources.add(createdInstanceId);
        tags.add(nameTag);
        
        CreateTagsRequest ctr = new CreateTagsRequest(resources, tags);
        ec2.createTags(ctr);
        
    	 /*********************************************
         * 
         *  #10 Stop/Start an Instance
         *  
         *********************************************/
    	System.out.println("#10 Stop the Instance");
        List<String> instanceIds = new LinkedList<String>();
        instanceIds.add(createdInstanceId);
        
        //stop
        StopInstancesRequest stopIR = new StopInstancesRequest(instanceIds);
        //ec2.stopInstances(stopIR);
        
        //start
        StartInstancesRequest startIR = new StartInstancesRequest(instanceIds);
        //ec2.startInstances(startIR);
        
        
        /*********************************************
         * 
         *  #11 Terminate an Instance
         *  
         *********************************************/
        System.out.println("#11 Terminate the Instance");
        TerminateInstancesRequest tir = new TerminateInstancesRequest(instanceIds);
        //ec2.terminateInstances(tir);
        
                    
        /*********************************************
         *  
         *  #12 shutdown client object
         *  
         *********************************************/
        ec2.shutdown();
        
        
        
    } catch (AmazonServiceException ase) {
            System.out.println("Caught Exception: " + ase.getMessage());
            System.out.println("Reponse Status Code: " + ase.getStatusCode());
            System.out.println("Error Code: " + ase.getErrorCode());
            System.out.println("Request ID: " + ase.getRequestId());
    }

        
	}
	


}
